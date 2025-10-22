import gradio as gr
from gradio_client import Client, handle_file
from PIL import Image
import os
import time
import traceback

print("=" * 60)
print("Starting Virtual Try-On API...")
print("=" * 60)

# Global client variable
vton_client = None

def initialize_client():
    """Initialize IDM-VTON client with retry logic"""
    global vton_client
    max_retries = 3

    for attempt in range(max_retries):
        try:
            print(f"Connecting to IDM-VTON (attempt {attempt + 1}/{max_retries})...")
            vton_client = Client("yisol/IDM-VTON")
            print("✓ Successfully connected to IDM-VTON!")
            return True
        except Exception as e:
            print(f"Connection attempt {attempt + 1} failed: {str(e)}")
            if attempt < max_retries - 1:
                time.sleep(5)

    print("❌ Failed to connect to IDM-VTON after all retries")
    return False

# Initialize on startup
initialize_client()

def predict(body_image, clothing_image):
    """
    Virtual Try-On API endpoint with better error handling
    """
    try:
        print("\n" + "=" * 60)
        print("New try-on request received")
        print("=" * 60)

        # Check if client is initialized
        if vton_client is None:
            print("⚠️  Client not initialized, attempting to reconnect...")
            if not initialize_client():
                raise Exception("Cannot connect to IDM-VTON service. Please try again later.")

        # Validate inputs
        if body_image is None or clothing_image is None:
            raise Exception("Both body photo and clothing item are required")

        print(f"Body image size: {body_image.size}")
        print(f"Clothing image size: {clothing_image.size}")

        # Create temp directory
        os.makedirs('/tmp', exist_ok=True)

        # Save images with unique names to avoid conflicts
        timestamp = int(time.time())
        body_path = f'/tmp/body_{timestamp}.jpg'
        garment_path = f'/tmp/garment_{timestamp}.jpg'

        print("Saving images...")
        body_image.save(body_path, 'JPEG', quality=95)
        clothing_image.save(garment_path, 'JPEG', quality=95)

        print("Calling IDM-VTON API...")
        print("⏳ This may take 20-40 seconds...")

        # Call IDM-VTON API with error handling
        try:
            result = vton_client.predict(
                dict={
                    "background": handle_file(body_path),
                    "layers": [],
                    "composite": None
                },
                garm_img=handle_file(garment_path),
                garment_des="a clothing item",
                is_checked=True,
                is_checked_crop=False,
                denoise_steps=30,
                seed=42,
                api_name="/tryon"
            )
        except Exception as api_error:
            print(f"❌ IDM-VTON API error: {str(api_error)}")
            # Try to reinitialize client
            print("Attempting to reconnect...")
            initialize_client()
            raise Exception(f"IDM-VTON service error: {str(api_error)}")

        print(f"Result type: {type(result)}")

        # Load result image
        try:
            if isinstance(result, tuple) and len(result) > 0:
                result_path = result[0]
                print(f"Result path: {result_path}")
                result_image = Image.open(result_path)
            elif isinstance(result, str):
                result_image = Image.open(result)
            else:
                result_image = result
        except Exception as img_error:
            print(f"❌ Error loading result image: {str(img_error)}")
            raise Exception(f"Failed to load result image: {str(img_error)}")

        # Clean up temp files
        try:
            os.remove(body_path)
            os.remove(garment_path)
        except:
            pass

        print("✅ Try-on complete!")
        print("=" * 60 + "\n")

        return result_image

    except Exception as e:
        error_msg = str(e)
        print(f"\n❌ ERROR: {error_msg}")
        traceback.print_exc()
        print("=" * 60 + "\n")

        # Return error image instead of raising exception
        return create_error_image(error_msg)

def create_error_image(error_message):
    """Create an image with the error message"""
    from PIL import ImageDraw, ImageFont

    # Create error image
    img = Image.new('RGB', (768, 1024), color='#FFF3CD')
    draw = ImageDraw.Draw(img)

    # Draw error icon (red X)
    draw.line([(350, 450), (420, 520)], fill='#DC3545', width=10)
    draw.line([(420, 450), (350, 520)], fill='#DC3545', width=10)

    # Wrap error message
    words = error_message.split(' ')
    lines = []
    current_line = []

    for word in words:
        current_line.append(word)
        test_line = ' '.join(current_line)
        if len(test_line) > 40:
            lines.append(' '.join(current_line[:-1]))
            current_line = [current_line[-1]]

    if current_line:
        lines.append(' '.join(current_line))

    # Draw text
    y = 550
    for line in lines[:5]:  # Max 5 lines
        bbox = draw.textbbox((0, 0), line)
        text_width = bbox[2] - bbox[0]
        x = (768 - text_width) // 2
        draw.text((x, y), line, fill='#856404')
        y += 30

    return img

# Create Gradio interface
print("\nCreating Gradio interface...")

iface = gr.Interface(
    fn=predict,
    inputs=[
        gr.Image(type="pil", label="📸 Body Photo"),
        gr.Image(type="pil", label="👕 Clothing Item")
    ],
    outputs=gr.Image(type="pil", label="✨ Try-On Result"),
    title="🎨 Virtual Try-On API",
    description="""
    Upload a body photo and clothing item for AI-powered virtual try-on.

    ⚠️ **Important:**
    - Processing takes 20-40 seconds
    - Use clear, front-facing body photos
    - Clothing should be on plain background
    - Rate limits may apply

    **Powered by IDM-VTON**
    """,
    api_name="predict",
    examples=None,
    cache_examples=False
)

print("✓ Gradio interface created")

# Launch with better configuration
print("\n" + "=" * 60)
print("🚀 Launching server...")
print("=" * 60 + "\n")

iface.launch(
    server_name="0.0.0.0",
    server_port=7860,
    show_error=True,
    quiet=False
)
