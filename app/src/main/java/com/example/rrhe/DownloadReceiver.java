package com.example.rrhe;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import java.io.File;

public class DownloadReceiver extends BroadcastReceiver {
    private static final String TAG = "DownloadReceiver";

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onReceive(Context context, Intent intent) {
        String downloadUrl = intent.getStringExtra("download_url");
        String title = intent.getStringExtra("title");
        String messageBody = intent.getStringExtra("messageBody");

        if (downloadUrl != null && !downloadUrl.isEmpty()) {
            // Ensure the old RRHE.apk is deleted before downloading the new one
            boolean isDeleted = deleteOldApkFile(context);

            if (isDeleted) {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
                request.setTitle(title);
                request.setDescription(messageBody);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "RRHE.apk");

                DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                long downloadId = downloadManager.enqueue(request);

                // Register a receiver to listen for when the download completes
                BroadcastReceiver onComplete = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context ctxt, Intent intent) {
                        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                        if (downloadId == id) {
                            // Check the status of the download
                            if (isDownloadSuccessful(ctxt, downloadId)) {
                                // Download completed, start installation process
                                installAPK(context);
                            } else {
                                Toast.makeText(ctxt, "Download failed. Please check the URL.", Toast.LENGTH_LONG).show();
                                Log.e(TAG, "Failed to download the APK. The file might be invalid.");
                            }
                        }
                    }
                };

                context.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED);
            } else {
                Toast.makeText(context, "Failed to delete the old RRHE.apk file. New download may not work correctly.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to delete the old RRHE.apk file.");
            }
        }
    }

    private boolean deleteOldApkFile(Context context) {
        File apkFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "RRHE.apk");
        if (apkFile.exists()) {
            try {
                boolean deleted = apkFile.delete();
                if (deleted) {
                    Toast.makeText(context, "Old RRHE.apk file deleted successfully.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Old RRHE.apk file deleted successfully.");
                    return true;
                } else {
                    Log.e(TAG, "Failed to delete the old RRHE.apk file.");
                    Toast.makeText(context, "Failed to delete the old RRHE.apk file. It may be in use.", Toast.LENGTH_LONG).show();
                }
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException: " + e.getMessage(), e);
                Toast.makeText(context, "Failed to delete the old RRHE.apk file due to security restrictions: " + e.getMessage(), Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.e(TAG, "Exception: " + e.getMessage(), e);
                Toast.makeText(context, "Failed to delete the old RRHE.apk file due to an error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
            return false;
        }
        return true;
    }

    private boolean isDownloadSuccessful(Context context, long downloadId) {
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);

        try (Cursor cursor = downloadManager.query(query)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                if (DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(columnIndex)) {
                    String mimeType = downloadManager.getMimeTypeForDownloadedFile(downloadId);
                    Log.d(TAG, "Download completed. MIME type: " + mimeType);
                    // Ensure the downloaded file is an APK
                    return "application/vnd.android.package-archive".equals(mimeType);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking download status: " + e.getMessage(), e);
        }

        return false;
    }

    private void installAPK(Context context) {
        File apkFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "RRHE.apk");
        if (apkFile.exists()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (context.getPackageManager().canRequestPackageInstalls()) {
                    startApkInstallation(context, apkFile);
                } else {
                    requestInstallPermission(context);
                }
            } else {
                startApkInstallation(context, apkFile);
            }
        } else {
            Toast.makeText(context, "Download failed, APK not found.", Toast.LENGTH_LONG).show();
        }
    }

    private void startApkInstallation(Context context, File apkFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri apkUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", apkFile);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void requestInstallPermission(Context context) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.getPackageName()));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        Toast.makeText(context, "Please grant permission to install apps from unknown sources.", Toast.LENGTH_LONG).show();
    }
}
