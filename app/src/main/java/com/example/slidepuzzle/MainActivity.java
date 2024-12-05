package com.example.slidepuzzle;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.Manifest;

public class MainActivity extends AppCompatActivity {
    private GridLayout gridLayout;
    private ImageButton[][] buttons;
    private Bitmap[][] imagePieces;
    private int[][] grid;
    private int numRows = 3;
    private int numColumns = 3;
    private int selectedImageResId = R.drawable.diana_47; // Ảnh mặc định
    private Button btnViewRecords;
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private Bitmap selectedBitmap; // Thêm biến toàn cục để lưu ảnh đã chọn
    private long startTimeInMillis = 60000;
    private long remainingTimeInMillis = startTimeInMillis;
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
        findViewById(R.id.viewRecordsButton).setOnClickListener(v -> showRecordsDialog());
        timerTextView = findViewById(R.id.timerTextView);
        // Kiểm tra quyền đọc bộ nhớ
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Nếu không có quyền, yêu cầu quyền
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
        }


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
        // Tạo danh sách chứa tất cả giá trị trong grid
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numColumns; j++) {
                values.add(grid[i][j]);
            }
        }

        // Trộn danh sách ngẫu nhiên
        Collections.shuffle(values);

        // Kiểm tra nếu cần phải đảm bảo trạng thái có thể giải được
        if (!isSolvable(values)) {
            // Nếu không thể giải, hoán đổi hai giá trị bất kỳ (trừ 0)
            if (values.size() > 2) {
                int temp = values.get(0);
                values.set(0, values.get(1));
                values.set(1, temp);
            }
        }

        // Gán lại các giá trị từ danh sách vào grid
        int index = 0;
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numColumns; j++) {
                grid[i][j] = values.get(index++);
            }
        }

        // Cập nhật giao diện người dùng
        updateButtons();
    }

    private boolean isSolvable(List<Integer> values) {
        int inversions = 0;
        int gridSize = values.size();

        for (int i = 0; i < gridSize; i++) {
            for (int j = i + 1; j < gridSize; j++) {
                if (values.get(i) != 0 && values.get(j) != 0 && values.get(i) > values.get(j)) {
                    inversions++;
                }
            }
        }

        // Nếu số hàng lẻ, hoán vị phải chẵn
        if (numRows % 2 == 1) {
            return inversions % 2 == 0;
        } else {
            // Nếu số hàng chẵn, kiểm tra vị trí ô trống (tính từ dưới lên)
            int blankRowFromBottom = numRows - (values.indexOf(0) / numColumns);
            return (inversions % 2 == 0) == (blankRowFromBottom % 2 == 1);
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
        stopTimer();

        long completionTime = startTimeInMillis - remainingTimeInMillis;
        showWinDialog(completionTime);
    }

    private void showHintDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Hint");

        ImageView hintImage = new ImageView(this);
        if (selectedBitmap != null) {
            // Hiển thị ảnh từ thư viện
            hintImage.setImageBitmap(selectedBitmap);
        } else {
            // Hiển thị ảnh mặc định
            hintImage.setImageBitmap(BitmapFactory.decodeResource(getResources(), selectedImageResId));
        }
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
            // Kiểm tra quyền đọc bộ nhớ
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // Nếu không có quyền, yêu cầu quyền
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
            }

            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(intent, 100);
        });

        EditText rowsInput = dialogView.findViewById(R.id.rowsInput);
        EditText columnsInput = dialogView.findViewById(R.id.columnsInput);
        Switch settingsSwitch = dialogView.findViewById(R.id.settingsSwitch); // Lấy Switch từ dialog
        //time swtich
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            try {
                // Lấy bitmap từ URI
                selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);

                // Cập nhật trò chơi với ảnh mới
                imagePieces = splitImage(selectedBitmap, numRows, numColumns);
                restartGameWithBitmap(); // Khởi động lại với bitmap mới
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void restartGameWithBitmap() {
        if (selectedBitmap == null) {
            restartGame(); // Nếu không có ảnh từ gallery, dùng ảnh mặc định
            return;
        }

        grid = new int[numRows][numColumns];
        buttons = new ImageButton[numRows][numColumns];

        int count = 1;
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numColumns; j++) {
                grid[i][j] = count++;
            }
        }
        grid[numRows - 1][numColumns - 1] = 0;

        imagePieces = splitImage(selectedBitmap, numRows, numColumns); // Dùng ảnh mới
        initializeGrid();
    }

    private void startTimer(TextView timerTextView) {
        countDownTimer = new CountDownTimer(startTimeInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingTimeInMillis = millisUntilFinished; // Update remaining time
                int seconds = (int) (millisUntilFinished / 1000);
                String time = String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60);
                timerTextView.setText(time); // Hiển thị thời gian
            }

            @Override
            public void onFinish() {
                // Khi thời gian kết thúc
                timerTextView.setText("00:00");
                Toast.makeText(MainActivity.this, "Game Over!!!", Toast.LENGTH_SHORT).show();
            }
        };
        countDownTimer.start(); // Bắt đầu đếm ngược
        Toast.makeText(this, "Timer started", Toast.LENGTH_SHORT).show();
    }


    private void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel(); // Dừng lại nếu có timer đang chạy
        }
        isTimerRunning = false;
        Toast.makeText(this,"Timer stopped",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 101) { // Kiểm tra permission yêu cầu
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Quyền được cấp, mở gallery
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                startActivityForResult(intent, 100);
            } else {
                Toast.makeText(this, "Bạn cần cấp quyền để truy cập gallery!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showWinDialog(long completionTime) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Congratulations!");

        // Add EditText to enter player name
        EditText input = new EditText(this);
        input.setHint("Enter your name");
        builder.setView(input);

        // Positive button to save the record
        builder.setPositiveButton("Save", (dialog, which) -> {
            String playerName = input.getText().toString().trim();
            if (!playerName.isEmpty()) {
                saveRecord(playerName, completionTime); // Save the record with the name and completion time
            }
            dialog.dismiss();
        });

        // Negative button to cancel
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        // Create and show the dialog
        builder.create().show();
    }

    private void saveRecord(String playerName, long completionTime) {
        // Format the completion time as hh:mm:ss
        String formattedTime = formatTime(completionTime);

        // Get SharedPreferences and prepare to save the record
        SharedPreferences sharedPreferences = getSharedPreferences("GameRecords", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Save the record with the player's name as the key and the time as the value
        String record = playerName + " - " + numColumns + " x " + numRows + " - " + formattedTime + "s";
        editor.putString(playerName, record);  // Store name and time

        editor.apply();  // Apply the changes
        Toast.makeText(this, "Record saved!", Toast.LENGTH_SHORT).show();
    }

    private String formatTime(long timeMillis) {
        int seconds = (int) (timeMillis / 1000) % 60;
        int minutes = (int) ((timeMillis / (1000 * 60)) % 60);
        int hours = (int) ((timeMillis / (1000 * 60 * 60)) % 24);
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void showRecordsDialog() {
        SharedPreferences sharedPreferences = getSharedPreferences("GameRecords", MODE_PRIVATE);
        Map<String, ?> allRecords = sharedPreferences.getAll();

        if (allRecords.isEmpty()) {
            Toast.makeText(this, "Không có kỷ lục nào!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo AlertDialog.Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Danh sách Kỷ Lục");

        // Dùng StringBuilder để tạo danh sách kỷ lục
        StringBuilder records = new StringBuilder();
        for (Map.Entry<String, ?> entry : allRecords.entrySet()) {
            String record = entry.getKey() + " - " + entry.getValue().toString();
            records.append(record).append("\n");
        }

        // Tạo layout tùy chỉnh cho mỗi item trong dialog
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Tạo các mục và nút xóa cho từng kỷ lục
        for (Map.Entry<String, ?> entry : allRecords.entrySet()) {
            String record = entry.getKey() + " - " + entry.getValue().toString();

            // Tạo TextView để hiển thị kỷ lục
            TextView recordTextView = new TextView(this);
            recordTextView.setText(record);
            recordTextView.setPadding(0, 10, 0, 10);

            // Tạo Button để xóa kỷ lục
            Button deleteButton = new Button(this);
            deleteButton.setText("Xóa");
            deleteButton.setOnClickListener(v -> {
                String playerName = entry.getKey(); // Lấy tên người chơi từ kỷ lục
                deleteRecord(playerName); // Gọi phương thức xóa kỷ lục
            });

            // Thêm TextView và Button vào layout
            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            itemLayout.setPadding(10, 10, 10, 10);
            itemLayout.addView(recordTextView);
            itemLayout.addView(deleteButton);

            // Thêm item vào layout chính
            layout.addView(itemLayout);
        }

        // Thiết lập layout cho AlertDialog
        builder.setView(layout);

        // Thêm nút đóng
        builder.setPositiveButton("Đóng", (dialog, which) -> dialog.dismiss());

        // Hiển thị dialog
        builder.create().show();
    }



    private void deleteRecord(String playerName) {
        // Lấy SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("GameRecords", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Xóa kỷ lục theo tên người chơi
        editor.remove(playerName);

        // Áp dụng thay đổi
        editor.apply();

        Toast.makeText(this, "Kỷ lục của " + playerName + " đã bị xóa!", Toast.LENGTH_SHORT).show();
    }



}




