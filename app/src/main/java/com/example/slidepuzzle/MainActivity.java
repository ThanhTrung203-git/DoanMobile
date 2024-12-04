package com.example.slidepuzzle;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private GridLayout gridLayout;
    private ImageButton[][] buttons;
    private Bitmap[][] imagePieces;
    private int[][] grid;
    private int numRows = 3;
    private int numColumns = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gridLayout = findViewById(R.id.gridLayout);
        // Khởi tạo GridLayout và số lượng hàng, cột
        gridLayout.setRowCount(numRows);
        gridLayout.setColumnCount(numColumns);

        // Tải ảnh và cắt thành các mảnh
        Bitmap sourceImage = BitmapFactory.decodeResource(getResources(), R.drawable.diana_47);
        if (sourceImage == null) {
            throw new RuntimeException("Source image not found. Ensure R.drawable.puzzle_image exists.");
        }
        imagePieces = splitImage(sourceImage, numRows, numColumns); // Khởi tạo imagePieces
        restartGame();

        // Xử lý sự kiện Reset
        findViewById(R.id.resetButton).setOnClickListener(v -> shuffleGrid());
        findViewById(R.id.hintButton).setOnClickListener(v -> showHintDialog());
        findViewById(R.id.settingsButton).setOnClickListener(v -> showSettingsDialog());
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
        hintImage.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.diana_47));
        hintImage.setAdjustViewBounds(true);

        builder.setView(hintImage);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_settings, null);
        builder.setView(dialogView);

        EditText rowsInput = dialogView.findViewById(R.id.rowsInput);
        EditText columnsInput = dialogView.findViewById(R.id.columnsInput);

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

        Bitmap sourceImage = BitmapFactory.decodeResource(getResources(), R.drawable.diana_47);
        imagePieces = splitImage(sourceImage, numRows, numColumns);

        initializeGrid();
    }
}




