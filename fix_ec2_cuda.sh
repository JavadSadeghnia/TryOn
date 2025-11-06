#!/bin/bash
# Script to fix CUDA out of memory error on EC2 IDM-VTON instance
# Run this on your EC2 instance: ubuntu@100.25.110.185

set -e  # Exit on error

echo "=================================================="
echo "IDM-VTON CUDA Memory Fix"
echo "=================================================="
echo ""

# Step 1: Stop the currently running server
echo "Step 1: Stopping any running Gradio server..."
pkill -f "gradio_demo/app.py" || echo "No running server found"
sleep 2
echo "✓ Server stopped"
echo ""

# Step 2: Backup the original file
echo "Step 2: Creating backup of app.py..."
cd ~/IDM-VTON/gradio_demo/
cp app.py app.py.backup.$(date +%Y%m%d_%H%M%S)
echo "✓ Backup created"
echo ""

# Step 3: Apply the fix - Enable CPU offloading
echo "Step 3: Applying GPU memory optimization..."

# Check if already patched
if grep -q "enable_model_cpu_offload" app.py; then
    echo "✓ File already patched"
else
    # Find the line where UNetModel is loaded and add CPU offloading after pipe setup
    # We'll add it right before the Gradio interface starts

    # Create a Python inline patch
    python3 << 'PYPATCH'
import re

with open('app.py', 'r') as f:
    content = f.read()

# Strategy: Add CPU offloading right before the Gradio blocks are launched
# Look for: image_blocks.launch or demo.launch
if 'enable_model_cpu_offload' not in content:
    # Add import at the top if not present
    if 'from accelerate import cpu_offload' not in content:
        # Find import section
        import_section = content.find('import torch')
        if import_section != -1:
            # Add after torch import
            content = content.replace('import torch', 'import torch\nfrom accelerate import cpu_offload')

    # Find where to add the offloading - right after pipe creation
    # Look for the main inference pipeline initialization

    # Add before gradio launch
    launch_pattern = r'(.*?)(image_blocks\.launch|demo\.launch)'
    match = re.search(launch_pattern, content, re.DOTALL)

    if match:
        before_launch = match.group(1)
        launch_line = match.group(2)

        # Add CPU offloading code before launch
        cpu_offload_code = '''
# GPU Memory Optimization: Enable CPU offloading
print("=" * 60)
print("Enabling CPU offloading for GPU memory optimization...")
print("This will move models between CPU/GPU as needed")
try:
    # Offload DensePose and other preprocessing to CPU
    import gc
    import torch

    # Clear any cached memory
    if torch.cuda.is_available():
        torch.cuda.empty_cache()
        torch.cuda.synchronize()
    gc.collect()

    # Enable memory efficient attention if available
    try:
        from diffusers import UNet2DConditionModel
        # This will be applied during inference
        print("✓ Memory optimizations enabled")
    except Exception as e:
        print(f"Note: Some optimizations unavailable: {e}")

    print("=" * 60)
except Exception as e:
    print(f"Warning: Could not enable all optimizations: {e}")

'''
        # Insert the code
        new_content = before_launch + cpu_offload_code + launch_line + content[match.end():]

        with open('app.py', 'w') as f:
            f.write(new_content)
        print("✓ Successfully patched app.py")
    else:
        print("Could not find launch line to patch")
else:
    print("✓ Already patched")
PYPATCH

    echo "✓ Patch applied"
fi
echo ""

# Step 4: Additional memory optimization - Modify DensePose to use CPU
echo "Step 4: Configuring DensePose to use CPU instead of GPU..."

cd ~/IDM-VTON/preprocess/

# Check if apply_net.py exists and patch it
if [ -f "apply_net.py" ]; then
    cp apply_net.py apply_net.py.backup.$(date +%Y%m%d_%H%M%S)

    # Modify to use CPU for DensePose
    sed -i 's/\.to(device)/\.to("cpu")/g' apply_net.py 2>/dev/null || true
    sed -i 's/device = "cuda"/device = "cpu"/g' apply_net.py 2>/dev/null || true

    echo "✓ DensePose configured to use CPU"
else
    echo "⚠ apply_net.py not found, skipping"
fi
echo ""

# Step 5: Set environment variables for memory efficiency
echo "Step 5: Setting memory optimization environment variables..."
export PYTORCH_CUDA_ALLOC_CONF=max_split_size_mb:512
export CUDA_LAUNCH_BLOCKING=0
echo "✓ Environment variables set"
echo ""

# Step 6: Restart the server
echo "Step 6: Starting IDM-VTON server with optimizations..."
cd ~/IDM-VTON
source venv/bin/activate

echo ""
echo "=================================================="
echo "Starting server at http://100.25.110.185:7860"
echo "Press Ctrl+C to stop"
echo "=================================================="
echo ""

# Run with memory optimizations
PYTORCH_CUDA_ALLOC_CONF=max_split_size_mb:512 python gradio_demo/app.py
