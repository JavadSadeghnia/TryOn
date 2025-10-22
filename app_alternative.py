import gradio as gr
from gradio_client import Client, handle_file
from PIL import Image
import os
import time
import traceback

print("=" * 60)
print("Starting Virtual Try-On API with Multiple Backends...")
print("=" * 60)

# Try multiple virtual try-on spaces to avoid rate limits
TRYON_SPACES = [
    "yisol/IDM-VTON",
    "Nymbo/Virtual-Try-On",
    "levihsu/OOTDiffusion",
]

vton_client = None
current_space = None

def initialize_client():
    """Try to connect to available virtual try-on spaces"""
    global vton_client, current_space

    for space in TRYON_SPACES:
        try:
            print(f"Attempting to connect to {space}...")
            vton_client = Client(space)
            current_space = space
            print(f"✓ Successfully connected to {space}")
            return True
        except Exception as e:
            print(f"✗ Failed to connect to {space}: {str(e)}")
            continue

    print("❌ Could not connect to any virtual try-on service")
    return False

# Initialize on startup
initialize_client()

def predict_idm_vton(body_image, clothing_image):
    """Call IDM-VTON API"""
    body_path = '/tmp/body.jpg'
    garment_path = '/tmp/garment.jpg'

    body_image.save(body_path, 'JPEG', quality=95)
    clothing_image.save(garment_path, 'JPEG', quality=95)

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

    if isinstance(result, tuple) and len(result) > 0:
        return Image.open(result[0])
    return Image.open(result)

def predict_nymbo(body_image, clothing_image):
    """Call Nymbo Virtual-Try-On API"""
    body_path = '/tmp/body.jpg'
    garment_path = '/tmp/garment.jpg'

    body_image.save(body_path, 'JPEG')
    clothing_image.save(garment_path, 'JPEG')

    result = vton_client.predict(
        handle_file(body_path),
        handle_file(garment_path),
        api_name="/predict"
    )

    if isinstance(result, str):
        return Image.open(result)
    return result

def predict(body_image, clothing_image):
    """
    Virtual Try-On with automatic fallback to different services
    """
    try:
        print("\n" + "=" * 60)
        print("New try-on request received")
        print("=" * 60)

        if vton_client is None:
            print("⚠️ No client available, attempting to reconnect...")
            if not initialize_client():
                raise Exception("All virtual try-on services are currently unavailable. Please try again in a few minutes.")

        if body_image is None or clothing_image is None:
            raise Exception("Both body photo and clothing item are required")

        print(f"Using service: {current_space}")
        print(f"Body image: {body_image.size}")
        print(f"Clothing image: {clothing_image.size}")

        os.makedirs('/tmp', exist_ok=True)

        print("Processing with AI... (20-40 seconds)")

        # Try the current space
        try:
            if current_space == "yisol/IDM-VTON":
                result_image = predict_idm_vton(body_image, clothing_image)
            elif current_space == "Nymbo/Virtual-Try-On":
                result_image = predict_nymbo(body_image, clothing_image)
            else:
                # Generic prediction
                body_path = '/tmp/body.jpg'
                garment_path = '/tmp/garment.jpg'
                body_image.save(body_path, 'JPEG')
                clothing_image.save(garment_path, 'JPEG')

                result = vton_client.predict(
                    handle_file(body_path),
                    handle_file(garment_path),
                    api_name="/predict"
                )
                result_image = Image.open(result[0] if isinstance(result, tuple) else result)

            print("✅ Try-on complete!")
            print("=" * 60 + "\n")
            return result_image

        except Exception as api_error:
            error_str = str(api_error)
            print(f"❌ Error with {current_space}: {error_str}")

            # If rate limit error, try to use another space
            if "429" in error_str or "Too Many Requests" in error_str:
                print("⚠️ Rate limit hit, trying alternative service...")

                # Try next space
                if initialize_client():
                    print(f"Switched to {current_space}, retrying...")
                    return predict(body_image, clothing_image)  # Recursive retry with new service
                else:
                    raise Exception("All services are rate-limited. Please try again in 1-2 minutes.")
            else:
                raise api_error

    except Exception as e:
        error_msg = str(e)
        print(f"\n❌ FINAL ERROR: {error_msg}")
        traceback.print_exc()
        print("=" * 60 + "\n")

        return create_error_image(error_msg)

def create_error_image(error_message):
    """Create an image with the error message"""
    from PIL import ImageDraw

    img = Image.new('RGB', (768, 1024), color='#FFF3CD')
    draw = ImageDraw.Draw(img)

    # Draw error icon
    draw.line([(350, 450), (420, 520)], fill='#DC3545', width=10)
    draw.line([(420, 450), (350, 520)], fill='#DC3545', width=10)

    # Wrap error text
    words = error_message.split(' ')
    lines = []
    current_line = []

    for word in words:
        current_line.append(word)
        if len(' '.join(current_line)) > 45:
            lines.append(' '.join(current_line[:-1]))
            current_line = [current_line[-1]]

    if current_line:
        lines.append(' '.join(current_line))

    # Draw text
    y = 550
    for line in lines[:6]:
        bbox = draw.textbbox((0, 0), line)
        text_width = bbox[2] - bbox[0]
        x = (768 - text_width) // 2
        draw.text((x, y), line, fill='#856404')
        y += 28

    # Add retry message
    retry_text = "Please try again in 1-2 minutes"
    bbox = draw.textbbox((0, 0), retry_text)
    text_width = bbox[2] - bbox[0]
    x = (768 - text_width) // 2
    draw.text((x, y + 20), retry_text, fill='#007BFF')

    return img

# Create interface
print("\nCreating Gradio interface...")

iface = gr.Interface(
    fn=predict,
    inputs=[
        gr.Image(type="pil", label="📸 Body Photo"),
        gr.Image(type="pil", label="👕 Clothing Item")
    ],
    outputs=gr.Image(type="pil", label="✨ Try-On Result"),
    title="🎨 Virtual Try-On API (Multi-Backend)",
    description="""
    Upload a body photo and clothing item for AI-powered virtual try-on.

    ⚠️ **Important:**
    - Processing takes 20-40 seconds
    - Uses multiple AI services with automatic fallback
    - If you see a rate limit error, wait 1-2 minutes and try again
    - Clear, front-facing photos work best

    **Powered by IDM-VTON and other AI services**
    """,
    api_name="predict"
)

print("✓ Gradio interface created")
print("\n" + "=" * 60)
print("🚀 Launching server with multi-backend support...")
print("=" * 60 + "\n")

iface.launch(
    server_name="0.0.0.0",
    server_port=7860,
    show_error=True
)
