#!/bin/bash
# Quick Fix Commands - Copy and paste these into your EC2 terminal
# EC2: ubuntu@100.25.110.185

# ============================================
# QUICK FIX FOR CUDA OUT OF MEMORY
# ============================================

# Stop current server
echo "Stopping server..."
pkill -f "gradio_demo/app.py"
sleep 2

# Navigate to IDM-VTON
cd ~/IDM-VTON

# Backup files
echo "Creating backups..."
cp preprocess/apply_net.py preprocess/apply_net.py.backup 2>/dev/null || true
cp preprocess/openpose/run_openpose.py preprocess/openpose/run_openpose.py.backup 2>/dev/null || true
cp preprocess/humanparsing/run_parsing.py preprocess/humanparsing/run_parsing.py.backup 2>/dev/null || true

# Fix DensePose - Move to CPU
echo "Configuring DensePose for CPU..."
if [ -f "preprocess/apply_net.py" ]; then
    sed -i 's/"cuda"/"cpu"/g' preprocess/apply_net.py
    sed -i 's/\.cuda()/\.cpu()/g' preprocess/apply_net.py
fi

# Fix OpenPose - Move to CPU
echo "Configuring OpenPose for CPU..."
if [ -f "preprocess/openpose/run_openpose.py" ]; then
    sed -i 's/device = torch.device("cuda")/device = torch.device("cpu")/g' preprocess/openpose/run_openpose.py
    sed -i 's/\.to("cuda")/\.to("cpu")/g' preprocess/openpose/run_openpose.py
fi

# Fix HumanParsing - Move to CPU
echo "Configuring HumanParsing for CPU..."
if [ -f "preprocess/humanparsing/run_parsing.py" ]; then
    sed -i 's/device = torch.device("cuda")/device = torch.device("cpu")/g' preprocess/humanparsing/run_parsing.py
    sed -i 's/\.to("cuda")/\.to("cpu")/g' preprocess/humanparsing/run_parsing.py
fi

# Activate environment
echo "Activating virtual environment..."
source venv/bin/activate

# Set memory optimization
echo "Setting memory optimizations..."
export PYTORCH_CUDA_ALLOC_CONF=max_split_size_mb:512

# Start server
echo ""
echo "=================================================="
echo "Starting IDM-VTON server with CPU preprocessing"
echo "Server will run at: http://100.25.110.185:7860"
echo "=================================================="
echo ""

python gradio_demo/app.py
