package com.example.slidepuzzle;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private GridLayout gridLayout;
    private ImageButton[][] buttons;
    private Bitmap[][] imagePieces;
    private int[][] grid;
    private int numRows = 3;
    private int numColumns = 3;
    private int selectedImageResId = R.drawable.diana_47; // Ảnh mặc định

    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;

    private TextView timerTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gridLayout = findViewById(R.id.gridLayout);
        // Khởi tạo GridLayout và số lượng hàng, cột
        gridLayout.setRowCount(numRows);
        gridLayout.setColumnCount(numColumns);

        // Tải ảnh và cắt thành các mảnh
        Bitmap sourceImage = BitmapFactory.decodeResource(getResources(), selectedImageResId);
        if (sourceImage == null) {
            throw new RuntimeException("Source image not found. Ensure R.drawable.puzzle_image exists.");
        }
        imagePieces = splitImage(sourceImage, numRows, numColumns); // Khởi tạo imagePieces
        restartGame();

        // Xử lý sự kiện Reset
        findViewById(R.id.resetButton).setOnClickListener(v -> shuffleGrid());
        findViewById(R.id.hintButton).setOnClickListener(v -> showHintDialog());
        findViewById(R.id.settingsButton).setOnClickListener(v -> showSettingsDialog());
        timerTextView = findViewById(R.id.timerTextView);

    }

    private Bitmap[][] splitImage(Bitmap source, int rows, int cols) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        // Kích thước của từng mảnh ghép
        int pieceWidth = sourceWidth / cols;
        int pieceHeight = sourceHeight / rows;

        Bitmap[][] pieces = new Bitmap[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // Xác định tọa độ và kích thước của từng mảnh
                int x = j * pieceWidth;
                int y = i * pieceHeight;

                // Nếu là mảnh cuối cùng ở hàng hoặc cột, điều chỉnh kích thước
                int width = (j == cols - 1) ? sourceWidth - x : pieceWidth;
                int height = (i == rows - 1) ? sourceHeight - y : pieceHeight;

                // Cắt mảnh ghép từ ảnh nguồn
                pieces[i][j] = Bitmap.createBitmap(source, x, y, width, height);
            }
        }

        return pieces;
    }



    private void initializeGrid() {
        // Đảm bảo xóa toàn bộ các thành phần trước khi thêm mới
        gridLayout.removeAllViews();

        // Thiết lập lại số hàng và số cột
        gridLayout.setRowCount(numRows);
        gridLayout.setColumnCount(numColumns);

        // Tính toán kích thước ô dựa trên GridLayout
        int buttonWidth = gridLayout.getWidth() / numColumns;
        int buttonHeight = gridLayout.getHeight() / numRows;

        buttons = new ImageButton[numRows][numColumns]; // Khởi tạo lại mảng nút

        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numColumns; j++) {
                ImageButton button = new ImageButton(this);

                // Thiết lập LayoutParams chính xác
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = buttonWidth;
                params.height = buttonHeight;
                params.rowSpec = GridLayout.spec(i); // Đảm bảo chỉ số hàng không vượt quá numRows
                params.columnSpec = GridLayout.spec(j); // Đảm bảo chỉ số cột không vượt quá numColumns

                // Giảm khoảng cách (margin)
                int marginInPx = (int) (2 * getResources().getDisplayMetrics().density); // 2dp -> px
                params.setMargins(marginInPx, marginInPx, marginInPx, marginInPx);

                button.setLayoutParams(params);
                button.setScaleType(ImageView.ScaleType.FIT_XY);

                // Gán sự kiện click
                button.setOnClickListener(new ButtonClickListener(i, j));

                // Thêm nút vào GridLayout
                gridLayout.addView(button);
                buttons[i][j] = button;
            }
        }

        // Cập nhật giao diện nút với trạng thái ban đầu
        updateButtons();
    }

    private void updateButtons() {
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numColumns; j++) {
                if (grid[i][j] == 0) {
                    buttons[i][j].setImageDrawable(null); // Ô trống
                } else {
                    int pieceIndex = grid[i][j] - 1;
                    int pieceRow = pieceIndex / numColumns;
                    int pieceCol = pieceIndex % numColumns;

                    // Đảm bảo mảnh ghép lấy đúng từ imagePieces
                    buttons[i][j].setImageBitmap(imagePieces[pieceRow][pieceCol]);
                }
            }
        }
    }

    private class ButtonClickListener implements View.OnClickListener {
        private final int row, col;

        public ButtonClickListener(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public void onClick(View v) {
            moveTile(row, col);
        }
    }

    private void moveTile(int row, int col) {
        if ((row > 0 && grid[row - 1][col] == 0) ||
                (row < numRows - 1 && grid[row + 1][col] == 0) ||
                (col > 0 && grid[row][col - 1] == 0) ||
                (col < numColumns - 1 && grid[row][col + 1] == 0)) {

            // Hoán đổi ô với ô trống
            int temp = grid[row][col];
            if (row > 0 && grid[row - 1][col] == 0) grid[row - 1][col] = temp;
            else if (row < numRows - 1 && grid[row + 1][col] == 0) grid[row + 1][col] = temp;
            else if (col > 0 && grid[row][col - 1] == 0) grid[row][col - 1] = temp;
            else if (col < numColumns - 1 && grid[row][col + 1] == 0) grid[row][col + 1] = temp;
            grid[row][col] = 0;

            updateButtons();
            checkWin();
        }
    }

    private void shuffleGrid() {
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            int row = random.nextInt(numRows);
            int col = random.nextInt(numColumns);
            moveTile(row, col);
        }
    }

    private void checkWin() {
        int count = 1;
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numColumns; j++) {
                if (i == numRows - 1 && j == numColumns - 1) {
                    if (grid[i][j] != 0) return;
                } else {
                    if (grid[i][j] != count++) return;
                }
            }
        }

        Toast.makeText(this, "You Win!", Toast.LENGTH_SHORT).show();
    }

    private void showHintDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Hint");

        ImageView hintImage = new ImageView(this);
        hintImage.setImageBitmap(BitmapFactory.decodeResource(getResources(), selectedImageResId));
        hintImage.setAdjustViewBounds(true);

        builder.setView(hintImage);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_settings, null);
        builder.setView(dialogView);

        MaterialButton selectImageButton = dialogView.findViewById(R.id.chooseImageButton);
        selectImageButton.setOnClickListener(v -> {
            showImageSelectionDialog();
        });
        MaterialButton selectImageGalleryButton = dialogView.findViewById(R.id.chooseImageGalleryButton);
        selectImageGalleryButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(intent, 100);
        });

        EditText rowsInput = dialogView.findViewById(R.id.rowsInput);
        EditText columnsInput = dialogView.findViewById(R.id.columnsInput);
        Switch settingsSwitch = dialogView.findViewById(R.id.settingsSwitch); // Lấy Switch từ dialog

        settingsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Khi Switch thay đổi trạng thái, bật/tắt đồng hồ
            if (isChecked) {
                // Bật tính năng thời gian
                startTimer(timerTextView); // Gọi startTimer từ MainActivity để bắt đầu đếm thời gian
            } else {
                // Tắt tính năng thời gian
                stopTimer();
                timerTextView.setText("00:00"); // Reset thời gian trong TextView
            }
        });

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        dialogView.findViewById(R.id.applyButton).setOnClickListener(v -> {
            String rowsText = rowsInput.getText().toString();
            String columnsText = columnsInput.getText().toString();

            if (rowsText.isEmpty() || columnsText.isEmpty()) {
                Toast.makeText(this, "Please fill in both fields!", Toast.LENGTH_SHORT).show();
                return;
            }

            int newRows = Integer.parseInt(rowsText);
            int newColumns = Integer.parseInt(columnsText);

            if (newRows < 2 || newColumns < 2) {
                Toast.makeText(this, "Rows and Columns must be at least 2!", Toast.LENGTH_SHORT).show();
            } else {
                numRows = newRows;
                numColumns = newColumns;
                restartGame();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void restartGame() {
        grid = new int[numRows][numColumns];
        buttons = new ImageButton[numRows][numColumns];

        int count = 1;
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numColumns; j++) {
                grid[i][j] = count++;
            }
        }
        grid[numRows - 1][numColumns - 1] = 0;

        Bitmap sourceImage = BitmapFactory.decodeResource(getResources(), selectedImageResId);
        imagePieces = splitImage(sourceImage, numRows, numColumns);

        initializeGrid();
    }

    private void showImageSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose an Image");

        // Mảng ID của các ảnh
        int[] imageResIds = {
                R.drawable.image_1, R.drawable.image_2, R.drawable.image_3,
                R.drawable.image_4, R.drawable.image_5, R.drawable.image_6,
                R.drawable.image_7, R.drawable.image_8, R.drawable.image_9, R.drawable.image_10
        };

        // Tạo danh sách ảnh dưới dạng tên
        String[] imageNames = {"Image 1", "Image 2", "Image 3", "Image 4", "Image 5",
                "Image 6", "Image 7", "Image 8", "Image 9", "Image 10"};

        builder.setItems(imageNames, (dialog, which) -> {
            selectedImageResId = imageResIds[which]; // Lưu ID ảnh được chọn
            restartGame(); // Khởi động lại trò chơi với ảnh mới
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            // Lấy URI của ảnh được chọn
            Uri selectedImageUri = data.getData();

            try {
                // Chuyển đổi URI thành Bitmap
                Bitmap selectedImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);

                // Cập nhật ảnh mới vào mảng imagePieces và khởi động lại trò chơi
                imagePieces = splitImage(selectedImage, numRows, numColumns);
                restartGame();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startTimer(TextView timerTextView) {
        long startTimeInMillis = 10000;
        countDownTimer = new CountDownTimer(startTimeInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Cập nhật thời gian hiển thị
                int seconds = (int) (millisUntilFinished / 1000);
                String time = String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60);
                timerTextView.setText(time); // Hiển thị thời gian
            }

            @Override
            public void onFinish() {
                // Khi thời gian kết thúc
                timerTextView.setText("00:00");
                Toast.makeText(MainActivity.this, "Timer finished", Toast.LENGTH_SHORT).show();
            }
        };
        countDownTimer.start(); // Bắt đầu đếm ngược
        Toast.makeText(this,"Timer started",Toast.LENGTH_SHORT).show();
    }

    private void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel(); // Dừng lại nếu có timer đang chạy
        }
        isTimerRunning = false;
        Toast.makeText(this,"Timer stopped",Toast.LENGTH_SHORT).show();
    }

}




