package com.pultec.tobyfoxundertale;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.app.Activity;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainTVActivity extends Activity {

    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final int MANAGE_STORAGE_PERMISSION_CODE = 101;
    private SharedPreferences sharedPreferences;
    private String oggFolderPath;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_android_tv); // Asegúrate de usar el layout correcto para tu actividad
        sharedPreferences = getSharedPreferences("your_preferences", MODE_PRIVATE);
        if (sharedPreferences.getString("oggFolderPath", null) == null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("oggFolderPath", "/storage/emulated/0/undertale");
            editor.apply();
        }
        checkAndRequestStoragePermission();
    }

    private void checkAndRequestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 (API 33) o superior
            if (!isManageExternalStorageGranted()) {
                requestManageExternalStoragePermission();
            } else {
                onStoragePermissionGranted();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6.0 a 12
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Solicitar permisos de almacenamiento
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
            } else {
                onStoragePermissionGranted();
            }
        } else {
            // No es necesario solicitar permisos en versiones anteriores de Android
            onStoragePermissionGranted();
        }
    }

    private boolean isManageExternalStorageGranted() {
        // Verificar si el permiso MANAGE_EXTERNAL_STORAGE está otorgado
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11 (API 30) o superior
            return Environment.isExternalStorageManager();
        }
        return false;
    }

    private void requestManageExternalStoragePermission() {
        // Abrir la configuración de permisos para conceder MANAGE_EXTERNAL_STORAGE
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, MANAGE_STORAGE_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onStoragePermissionGranted();
            } else {
                //Toast.makeText(this, "Permiso de almacenamiento denegado", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MANAGE_STORAGE_PERMISSION_CODE) {
            if (isManageExternalStorageGranted()) {
                onStoragePermissionGranted();
            } else {
                //Toast.makeText(this, "Permiso de administración de almacenamiento denegado", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void onStoragePermissionGranted() {
        // Lógica que debe ejecutarse cuando el permiso de almacenamiento es otorgado
        //Toast.makeText(this, "Permiso de almacenamiento concedido", Toast.LENGTH_LONG).show();
        createDirectories();
    }

    private void createDirectories() {
        File undertaleDir = new File(Environment.getExternalStorageDirectory(), "undertale");
        if (!undertaleDir.exists()) {
            undertaleDir.mkdirs(); // Crea la carpeta undertale
        }

        // Crear subcarpetas
        new File(undertaleDir, "save files").mkdirs();

        // Verificar si game.droid existe
        File gameDroidFile = new File(getFilesDir(), "game.droid");
        if (gameDroidFile.exists()) {
            launchMainActivity();
        } else {
            downloadFileFromGoogleDrive("https://drive.google.com/uc?export=download&id=11csIKR7bZlGiKy-JeGUlpBbRWiZGceJC");
        }
    }

    private void copyOggFilesToCacheWithProgress() {
        SharedPreferences sharedPreferences = getSharedPreferences("your_preferences", MODE_PRIVATE);
        String oggFolderPath = sharedPreferences.getString("oggFolderPath", null);
        File oggFolder = new File(oggFolderPath);

        if (oggFolder != null && oggFolder.exists() && oggFolder.isDirectory()) {
            File[] oggFiles = oggFolder.listFiles((dir, name) -> name.endsWith(".ogg"));

            if (oggFiles != null && oggFiles.length > 0) {
                // Crear y mostrar el AlertDialog
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("loading files")
                        .setMessage("Please wait...")
                        .setCancelable(false);
                final AlertDialog dialog = builder.create();
                dialog.show();

                // Usar AsyncTask para copiar archivos
                new AsyncTask<Void, Integer, Boolean>() {
                    @Override
                    protected Boolean doInBackground(Void... voids) {
                        long totalBytesCopied = 0;
                        long totalBytesToCopy = 0;

                        // Calcular el tamaño total de los archivos a copiar
                        for (File file : oggFiles) {
                            totalBytesToCopy += file.length();
                        }

                        // Copiar archivos
                        for (File file : oggFiles) {
                            try (InputStream inputStream = new FileInputStream(file);
                                 OutputStream outputStream = new FileOutputStream(new File(getCacheDir(), file.getName()))) {

                                byte[] buffer = new byte[1024];
                                int bytesRead;
                                long bytesReadTotal = 0;

                                while ((bytesRead = inputStream.read(buffer)) != -1) {
                                    outputStream.write(buffer, 0, bytesRead);
                                    bytesReadTotal += bytesRead;
                                    totalBytesCopied += bytesRead;

                                    // Calcular y publicar el progreso
                                    publishProgress((int) ((totalBytesCopied * 100) / totalBytesToCopy));
                                }
                            } catch (IOException e) {
                                Log.e("CopyFiles", "Error copying file: " + file.getName(), e);
                                return false; // Si ocurre un error, termina la tarea
                            }
                        }
                        return true; // Copia exitosa
                    }

                    @Override
                    protected void onProgressUpdate(Integer... values) {
                        // Actualizar el mensaje del AlertDialog con el progreso
                        dialog.setMessage("loading files... " + values[0] + "% completed");
                    }

                    @Override
                    protected void onPostExecute(Boolean success) {
                        dialog.dismiss(); // Cerrar el AlertDialog
                        if (success) {
                            downloadFileFromGoogleDrive("https://drive.google.com/uc?export=download&id=11csIKR7bZlGiKy-JeGUlpBbRWiZGceJC");
                            //Toast.makeText(MainTVActivity.this, "Archivos copiados exitosamente.", Toast.LENGTH_LONG).show();
                        } else {
                            //Toast.makeText(MainTVActivity.this, "Error al copiar los archivos.", Toast.LENGTH_LONG).show();
                        }
                    }
                }.execute();
            } else {
                Toast.makeText(this, "no ogg files found", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "undertale folder no exists", Toast.LENGTH_LONG).show();
        }
    }


    private void downloadFileFromGoogleDrive(String fileUrl) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Downloading content for your TV")
                .setMessage("Please wait...")
                .setCancelable(false);
        final AlertDialog dialog = builder.create();
        dialog.show();

        // Cambia el tipo de AsyncTask a <Void, Integer, Boolean>
        new AsyncTask<Void, Integer, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(fileUrl).build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                    File outputFile = new File(getFilesDir(), "game.droid"); // Cambia el nombre según sea necesario
                    try (InputStream inputStream = response.body().byteStream();
                         FileOutputStream outputStream = new FileOutputStream(outputFile)) {

                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        long totalBytesRead = 0;
                        long fileSize = response.body().contentLength(); // Tamaño total del archivo

                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;
                            publishProgress((int) ((totalBytesRead * 100) / fileSize)); // Actualizar progreso
                        }
                    }
                    return true; // Descarga exitosa
                } catch (IOException e) {
                    Log.e("DownloadFile", "Error downloading file", e);
                    return false; // Descarga fallida
                }
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                dialog.setMessage("applying content... " + values[0] + "% completed");
            }

            @Override
            protected void onPostExecute(Boolean success) {
                dialog.dismiss();
                if (success) {
                    launchMainActivity();
                    Toast.makeText(MainTVActivity.this, "Done.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainTVActivity.this, "Fail.", Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }

    private void launchMainActivity() {
        Intent intent = new Intent(MainTVActivity.this, RunnerActivity.class);
        startActivity(intent);
        finish(); // Finaliza MainTVActivity
    }
}


