#!/bin/bash
set -e

# 1. Setup
DATA_DIR="/app/training_data"
cd $DATA_DIR
echo "----------------------------------------"
echo "Starting Tesseract Fine-Tuning Demo"
echo "----------------------------------------"

# 2. Download Base Model
if [ ! -f eng.traineddata ]; then
    echo "[1/5] Downloading eng.traineddata (best)..."
    wget -q https://github.com/tesseract-ocr/tessdata_best/raw/main/eng.traineddata
else
    echo "[1/5] eng.traineddata found."
fi

# 3. Extract LSTM
echo "[2/5] Extracting base LSTM weights..."
combine_tessdata -e eng.traineddata eng.lstm

# 3.5 Generate Synthetic Data (Fixes Segmentation Issues)
echo "[2.5/5] Generating Synthetic Training Image using text2image..."
# Using DejaVu Sans which is available in the container
text2image --text=eng.fci.exp0.gt.txt \
           --outputbase=eng.fci.exp0 \
           --font="DejaVu Sans" \
           --fonts_dir=/usr/share/fonts/truetype/dejavu \
           --ptsize 12

# 4. Generate LSTMF from Synthetic TIF
echo "[3/5] Generating Training Data (.lstmf)..."
tesseract eng.fci.exp0.tif eng.fci.exp0 --psm 6 lstm.train




# 5. Create Train List
echo "[4/5] Creating train_list.txt..."
ls *.lstmf > train_list.txt

# 6. Run Training
echo "[5/5] Running lstmtraining (50 iterations)..."
mkdir -p output
lstmtraining \
  --model_output output/fci_checkpoint \
  --continue_from eng.lstm \
  --traineddata eng.traineddata \
  --train_listfile train_list.txt \
  --max_iterations 50

echo "----------------------------------------"
echo "Training Success! Check 'tesseract_training/output' folder."
echo "----------------------------------------"
