package video.pano.demo.mixaudiocall;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class FileUtils {

    public static String getFileFromUri(Context context, Uri uri) {
        if (DocumentsContract.isDocumentUri(context, uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            String author = uri.getAuthority();
            if ("com.android.externalstorage.documents".equals(uri.getAuthority())){
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                String type = docId.split(":")[0];
                Uri contentUri = null;
                if (type.equalsIgnoreCase("image")) {
                    contentUri =  MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if (type.equalsIgnoreCase("audio")) {
                    contentUri =  MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                } else if (type.equalsIgnoreCase("video")) {
                    contentUri =  MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                }
                return getPath(context, contentUri, selection);
            } else if ("com.android.providers.media.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                        Long.parseLong(docId));
                return getPath(context, contentUri, null);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                if (docId.startsWith("raw:")) {
                    return docId.replaceFirst("raw:", "");
                }
                final Uri downloadUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(docId));
                return getPath(context, downloadUri, null);
            } else if ("content".equals(uri.getAuthority())) {
                return getPath(context, uri, null);
            }
        } else {
            return getPath(context, uri, null);
        }
        return "";
    }

    private static String getPath(Context context, Uri uri, String selection) {
        String path = null;
        Cursor cursor = context.getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                try {
                    path = cursor.getString(cursor.getColumnIndex("_data"));
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        path = CopyToTmpAndGetPath(context, uri);
                    } catch (Exception es) {
                        es.printStackTrace();
                    }
                }
            }
            cursor.close();
        } else {
            path = uri.getPath();
        }
        return path;
    }

    private static String CopyToTmpAndGetPath(Context context, Uri contentUri) {
        //copy file and send new file path
        String fileName = getFileName(contentUri);
        if (!TextUtils.isEmpty(fileName)) {
            File newFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC) + File.separator + fileName);
            copyFile(context, contentUri, newFile);
            return newFile.getAbsolutePath();
        }
        return null;
    }

    private static String getFileName(Uri uri) {
        if (uri == null) return null;
        String fileName = null;
        String path = uri.getPath();
        assert path != null;
        int cut = path.lastIndexOf('/');
        if (cut != -1) {
            fileName = path.substring(cut + 1);
        }
        return fileName;
    }

    private static void copyFile(Context context, Uri srcUri, File dstFile) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(srcUri);
            if (inputStream == null) return;
            OutputStream outputStream = new FileOutputStream(dstFile);
            copyStream(inputStream, outputStream);
            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int copyStream(InputStream input, OutputStream output) throws Exception, IOException {
        final int BUFFER_SIZE = 1024 * 2;
        byte[] buffer = new byte[BUFFER_SIZE];
        BufferedInputStream in = new BufferedInputStream(input, BUFFER_SIZE);
        BufferedOutputStream out = new BufferedOutputStream(output, BUFFER_SIZE);
        int count = 0, n = 0;
        try {
            while ((n = in.read(buffer, 0, BUFFER_SIZE)) != -1) {
                out.write(buffer, 0, n);
                count += n;
            }
            out.flush();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return count;
    }

}
