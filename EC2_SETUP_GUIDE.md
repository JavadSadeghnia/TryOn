# EC2 Setup Guide for OOTDiffusion

## Step-by-Step AWS EC2 Setup

### Step 1: Create AWS Account & Access EC2
1. Go to https://aws.amazon.com/
2. Sign in to AWS Console
3. Search for "EC2" in the services search bar
4. Click "EC2" to open the EC2 Dashboard

---

### Step 2: Launch EC2 Instance

#### 2.1 Click "Launch Instance"
- You'll see this button on the EC2 Dashboard

#### 2.2 Configure Instance Settings:

**Name:**
```
OOTDiffusion-TryOn
```

**Application and OS Images (Amazon Machine Image):**
- Select: **Ubuntu Server 22.04 LTS**
- Architecture: **64-bit (x86)**

**Instance Type:**
For OOTDiffusion, you need GPU:
- Recommended: **g4dn.xlarge** (most cost-effective with GPU)
  - 4 vCPUs
  - 16 GB RAM
  - 1 NVIDIA T4 GPU (16GB VRAM)
  - Cost: ~$0.526/hour (~$12.62/day if running 24/7)

Alternative if budget is tight:
- **g4dn.2xlarge** (better performance)
  - 8 vCPUs
  - 32 GB RAM
  - 1 NVIDIA T4 GPU
  - Cost: ~$0.752/hour

**Key Pair (login):**
- Click "Create new key pair"
- Name: `ootdiffusion-key`
- Key pair type: RSA
- Private key file format: `.pem`
- Click "Create key pair"
- **IMPORTANT:** Save the `.pem` file - you can't download it again!

**Network Settings:**
- Create security group: ✓
- Security group name: `ootdiffusion-sg`
- Description: `Security group for OOTDiffusion API`

**Configure Security Group Rules:**
1. SSH (Port 22) - Already added by default
   - Type: SSH
   - Source: My IP (or Anywhere for testing)

2. Click "Add security group rule"
   - Type: Custom TCP
   - Port range: `7860`
   - Source: Anywhere (0.0.0.0/0)
   - Description: Gradio API access

3. Click "Add security group rule" (optional for HTTPS)
   - Type: HTTPS
   - Port range: `443`
   - Source: Anywhere

**Storage:**
- Size: **50 GB** (minimum - OOTDiffusion models are ~10GB)
- Volume type: gp3 (default)

#### 2.3 Launch Instance
- Review all settings
- Click "Launch instance"
- Wait 2-3 minutes for instance to start

---

### Step 3: Connect to EC2 Instance

#### 3.1 Get Instance Details
- Go to EC2 Dashboard → Instances
- Select your instance
- Note the **Public IPv4 address** (e.g., 3.25.110.185)

#### 3.2 Connect via SSH

**On Mac/Linux:**
```bash
# Move to a safe location
mkdir -p ~/.ssh/aws-keys
mv ~/Downloads/ootdiffusion-key.pem ~/.ssh/aws-keys/

# Set permissions
chmod 400 ~/.ssh/aws-keys/ootdiffusion-key.pem

# Connect (replace with your IP)
ssh -i ~/.ssh/aws-keys/ootdiffusion-key.pem ubuntu@YOUR_EC2_PUBLIC_IP
```

**On Windows:**
- Use PuTTY or Windows Terminal with the .pem key
- Or use AWS EC2 Instance Connect from the console

---

### Step 4: Install Dependencies on EC2

Once connected to your EC2 instance:

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Python 3.10
sudo apt install python3.10 python3.10-venv python3-pip -y

# Install CUDA toolkit (for GPU)
wget https://developer.download.nvidia.com/compute/cuda/repos/ubuntu2204/x86_64/cuda-keyring_1.0-1_all.deb
sudo dpkg -i cuda-keyring_1.0-1_all.deb
sudo apt-get update
sudo apt-get -y install cuda-toolkit-12-1

# Install git
sudo apt install git -y

# Verify GPU
nvidia-smi
# You should see NVIDIA T4 GPU information
```

---

### Step 5: Install OOTDiffusion

```bash
# Clone OOTDiffusion repository
cd ~
git clone https://github.com/levihsu/OOTDiffusion.git
cd OOTDiffusion

# Create virtual environment
python3 -m venv venv
source venv/bin/activate

# Install PyTorch with CUDA
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu121

# Install OOTDiffusion requirements
pip install -r requirements.txt

# Install Gradio for API
pip install gradio

# Download model checkpoints
mkdir -p checkpoints
cd checkpoints

# Download models (this will take some time - ~10GB)
# Option 1: Using git lfs
git lfs install
git clone https://huggingface.co/levihsu/OOTDiffusion

# Or download specific files
# The models will be automatically downloaded on first run
```

---

### Step 6: Create Gradio API Script

Create a file `api_server.py`:

```bash
nano ~/OOTDiffusion/api_server.py
```

Paste this content:

```python
import gradio as gr
from PIL import Image
import torch
from pathlib import Path

# Import OOTDiffusion modules
from ootd.inference_ootd import OOTDiffusionInference

# Initialize model
model = OOTDiffusionInference()

def try_on(person_image, garment_image, category="upper_body"):
    """
    Virtual try-on function

    Args:
        person_image: PIL Image of person
        garment_image: PIL Image of garment
        category: "upper_body" or "lower_body" or "dresses"

    Returns:
        PIL Image of result
    """
    try:
        # Run inference
        result = model.inference(
            person_image=person_image,
            garment_image=garment_image,
            category=category,
            num_samples=1
        )

        return result[0]
    except Exception as e:
        print(f"Error: {e}")
        return None

# Create Gradio interface
with gr.Blocks(title="OOTDiffusion Virtual Try-On") as demo:
    gr.Markdown("# OOTDiffusion Virtual Try-On API")

    with gr.Row():
        with gr.Column():
            person_input = gr.Image(label="Person Photo", type="pil")
            garment_input = gr.Image(label="Garment Photo", type="pil")
            category_input = gr.Radio(
                choices=["upper_body", "lower_body", "dresses"],
                value="upper_body",
                label="Garment Category"
            )
            submit_btn = gr.Button("Try On", variant="primary")

        with gr.Column():
            result_output = gr.Image(label="Result")

    submit_btn.click(
        fn=try_on,
        inputs=[person_input, garment_input, category_input],
        outputs=result_output
    )

# Launch API
if __name__ == "__main__":
    demo.launch(
        server_name="0.0.0.0",  # Allow external access
        server_port=7860,
        share=False
    )
```

Save and exit (Ctrl+X, Y, Enter)

---

### Step 7: Run OOTDiffusion API

```bash
# Make sure you're in the OOTDiffusion directory
cd ~/OOTDiffusion
source venv/bin/activate

# Run the API server
python api_server.py
```

You should see:
```
Running on local URL:  http://0.0.0.0:7860
```

---

### Step 8: Test the API

Open your browser and go to:
```
http://YOUR_EC2_PUBLIC_IP:7860
```

You should see the Gradio interface!

---

### Step 9: Keep Server Running (Optional)

To keep the server running even after you disconnect:

```bash
# Install screen
sudo apt install screen -y

# Create a new screen session
screen -S ootdiffusion

# Run the server
cd ~/OOTDiffusion
source venv/bin/activate
python api_server.py

# Detach from screen: Press Ctrl+A then D

# To reattach later:
screen -r ootdiffusion
```

---

### Step 10: Get Your API URL

Your API endpoint will be:
```
http://YOUR_EC2_PUBLIC_IP:7860
```

Use this URL in your Android app!

---

## Cost Estimation

**g4dn.xlarge:**
- Hourly: $0.526
- Daily (24h): $12.62
- Monthly (730h): $384

**To Save Money:**
1. Stop instance when not in use (only pay for storage)
2. Use Spot Instances (up to 70% cheaper)
3. Set up auto-shutdown after inactivity

---

## Next Steps

1. ✅ EC2 instance running
2. ✅ OOTDiffusion installed
3. ✅ API accessible at http://YOUR_IP:7860
4. 🔄 Update Android app to use this API
5. 🔄 Test end-to-end flow

---

## Troubleshooting

**GPU not found:**
```bash
nvidia-smi
# Should show NVIDIA T4
```

**Port 7860 not accessible:**
- Check Security Group rules in EC2 console
- Ensure port 7860 is open to 0.0.0.0/0

**Out of memory:**
- Use smaller batch size
- Reduce image resolution
- Use g4dn.2xlarge instead

**Models not downloading:**
```bash
# Manually download from HuggingFace
huggingface-cli download levihsu/OOTDiffusion
```
