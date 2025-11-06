# IDM-VTON Quick Reference Guide

## SSH Connection

```bash
ssh -i tryon-key.pem ubuntu@100.25.110.185
```

## Start Server

```bash
cd ~/IDM-VTON
source venv/bin/activate
export PYTORCH_CUDA_ALLOC_CONF=expandable_segments:True
python gradio_demo/app.py
```

## Start Server in Background

```bash
screen -S idm-vton
cd ~/IDM-VTON
source venv/bin/activate
export PYTORCH_CUDA_ALLOC_CONF=expandable_segments:True
python gradio_demo/app.py
# Detach: Ctrl+A then D
```

## Manage Background Server

```bash
# List screens
screen -ls

# Reattach to server
screen -r idm-vton

# Kill server
screen -X -S idm-vton quit

# Or kill process directly
pkill -f "gradio_demo/app.py"
```

## Monitor GPU

```bash
# One-time check
nvidia-smi

# Live monitoring
watch -n 1 nvidia-smi
```

## Check Server Status

```bash
# Check if running
ps aux | grep app.py

# Check port
netstat -tulpn | grep 7860

# Check logs (if using screen)
screen -r idm-vton
```

## Access Interface

**URL**: http://100.25.110.185:7860

## Stop EC2 Instance (Save Money)

**Via AWS Console:**
1. Go to EC2 Dashboard
2. Select "NodeMonitoring IV"
3. Instance State → Stop

**Via CLI:**
```bash
aws ec2 stop-instances --instance-ids i-XXXXXXXXXX
```

## Performance

- **Processing Time**: ~30 seconds
- **Output Resolution**: 768x1024
- **Steps**: 20 (adjustable 15-40)
- **Cost per Request**: $0.00627

## Key Files on EC2

```
~/IDM-VTON/
├── gradio_demo/
│   └── app.py              # Main application (modified)
├── ckpt/
│   ├── densepose/          # 244MB
│   ├── openpose/           # 200MB
│   └── humanparsing/       # 510MB
├── unet/                   # 12GB
├── unet_encoder/           # 9.6GB
└── venv/                   # Python environment
```

## Costs

| Usage | Cost/Month |
|-------|------------|
| 8 hours/day | $181 |
| 12 hours/day | $271 |
| 24/7 | $549 |
| Stopped (storage only) | $15 |

## Emergency Commands

```bash
# If server hangs
pkill -9 -f app.py

# If GPU memory issue
nvidia-smi --gpu-reset

# Clear Python cache
find . -type d -name __pycache__ -exec rm -rf {} +

# Free disk space
du -sh ~/IDM-VTON/*
```

## Applied Optimizations

✓ DensePose on CPU (freed GPU memory)
✓ VAE slicing and tiling (memory efficient)
✓ Attention slicing (reduced memory)
✓ Steps reduced 30→20 (33% faster)
✓ xformers installed (10-15% faster)

**Total Speed Improvement**: 42% faster (52s → 30s)
