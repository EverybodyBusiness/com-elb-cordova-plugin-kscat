package com.darryncampbell.cordova.plugin.intent;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaActivity;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static android.os.Environment.getExternalStorageDirectory;
import static android.os.Environment.getExternalStorageState;

public class IntentShim extends CordovaPlugin {

    private final Map<BroadcastReceiver, CallbackContext> receiverCallbacks = new HashMap<>();

    private static final String LOG_TAG = "Cordova Intents Shim";
    private CallbackContext onNewIntentCallbackContext = null;
    private CallbackContext onActivityResultCallbackContext = null;

    private Intent deferredIntent = null;

   byte[] mRequestTelegram;

   byte[] mRequestTelegramReconnectKscat;

  public static byte FS  = (byte)0x1C;

  public static final int RESULT_OK = -1;
  public static final int RESULT_CANCELED = 0;

  public static String stringToAmount(String str, int length)
  {
    int strLength = str.getBytes().length;
    String temp = "";

    for (int i = strLength; i < length; i++)
    {
      temp = temp + "0";
    }

    temp = temp + str;
    return temp;
  }

  public static String dumpHexString(byte[] array) {
    return dumpHexString(array, 0, array.length);
  }
  private final static char[] HEX_DIGITS = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
  };
  public static String dumpHexString(byte[] array, int offset, int length) {
    StringBuilder result = new StringBuilder();

    byte[] line = new byte[8];
    int lineIndex = 0;

    for (int i = offset; i < offset + length; i++) {
      if (lineIndex == line.length) {
        for (int j = 0; j < line.length; j++) {
          if (line[j] > ' ' && line[j] < '~') {
            result.append(new String(line, j, 1));
          } else {
            result.append(".");
          }
        }

        result.append("\n");
        lineIndex = 0;
      }

      byte b = array[i];
      result.append(HEX_DIGITS[(b >>> 4) & 0x0F]);
      result.append(HEX_DIGITS[b & 0x0F]);
      result.append(" ");

      line[lineIndex++] = b;
    }

    for (int i = 0; i < (line.length - lineIndex); i++) {
      result.append("   ");
    }
    for (int i = 0; i < lineIndex; i++) {
      if (line[i] > ' ' && line[i] < '~') {
        result.append(new String(line, i, 1));
      } else {
        result.append(".");
      }
    }

    return result.toString();
  }

    public IntentShim() {

    }
  private void makeTelegramIC(String TID,String installment,String totAmt,String tax,String amt,String sign) {
    ByteBuffer bb = ByteBuffer.allocate(4096);

    bb.put((byte)0x02);                                                 // STX
    bb.put("IC".getBytes());                                            // 거래구분
    bb.put("01".getBytes());                                            // 업무구분
    bb.put("0200".getBytes());                                      // 전문구분(결제)
    bb.put("N".getBytes());                                             // 거래형태
    bb.put(TID.getBytes());                                        // 단말기번호
    for(int i=0; i< 4; i++) bb.put(" ".getBytes());                     // 업체정보
    for(int i=0; i<12; i++) bb.put(" ".getBytes());                     // 전문일련번호
    // bb.put("K".getBytes());                                          // POS Entry Mode   // MS
    bb.put("S".getBytes());                                             // POS Entry Mode   // IC
    for(int i=0; i<20; i++) bb.put(" ".getBytes());                     // 거래 고유 번호
    for(int i=0; i<20; i++) bb.put(" ".getBytes());                     // 암호화하지 않은 카드 번호
    bb.put("9".getBytes());                                             // 암호화여부
    bb.put("################".getBytes());
    bb.put("################".getBytes());
    for(int i=0; i<40; i++) bb.put(" ".getBytes());                     // 암호화 정보
    // bb.put("4330280486944821=17072011025834200000".getBytes());      // Track II - MS
    // bb.put("123456789012345612345=8911           ".getBytes());      // Track II - App카드
    for(int i=0; i<37; i++) bb.put(" ".getBytes());                     // Track II - IC
    bb.put(FS);                                                    // FS
    bb.put(installment.getBytes());                         // 할부개월

    bb.put(stringToAmount(totAmt, 12).getBytes());           // 총금액
    bb.put("000000000000".getBytes());    // 봉사료
    bb.put(stringToAmount(tax, 12).getBytes());           // 세금
    bb.put(stringToAmount(amt, 12).getBytes());     // 공급금액
    bb.put("000000000000".getBytes());                                  // 면세금액
    bb.put("AA".getBytes());                                            // Working Key Index
    for(int i=0; i<16; i++) bb.put("0".getBytes());                     // 비밀번호
    bb.put("            ".getBytes());                              // 원거래승인번호
    bb.put("      ".getBytes());                                    // 원거래승인일자
    for(int i=0; i<163; i++) bb.put(" ".getBytes());                    // 사용자정보 ~ DCC 환율조회 Data
    // EMV Data Length(4 bytes)
    // EMV Data
    //bb.put(" ".getBytes());                                             // 전자서명 유무
    // bb.put("N".getBytes());
    bb.put(sign.getBytes());
    //bb.put("S".getBytes());                                              // 전자서명 유무
    //bb.put("83".getBytes());                                          // 전자서명 암호화 Key Index


    //for(int i=0; i<16; i++) bb.put("0".getBytes());                   // 제품코드 및 버전        // KN1512021C000002
    //bb.put("KSPS2SP210600051".getBytes());
    //bb.put("0108".getBytes());
    //bb.put(String.format("%04d",  encBmpData.length()).getBytes());   // 전자서명 길이          // 0248
    //bb.put(encBmpData.getBytes());                                    // 전자서명              // 716634346E5567636D7737777643756E7666596D797934554647657A38764A784B2F7744545657554F72586341586A6954365441594E6E6F692B69412B572F49316B6D7072326744716C4B4B2F75624D6C6E684E6F346F4B7A54413757314578774F5975746E5A726759547166357244466238356B37516D50484D3057416B59547153755959432F71326D414E6B613042543841555A4B795556544179685341464C327442565857772F396A4C34554F306574594C696B54596535794C486858437A4568756B6A434448766D6F4B3449694D6D32753570507739654B442F564D387A312F594D6A3966787A4D396A6753435A6F6B76773D3D

    //bb.put(Util.toByte("716634346E5567636D7737777643756E7666596D797934554647657A38764A784B2F7744545657554F72586341586A6954365441594E6E6F692B69412B572F49316B6D7072326744716C4B4B2F75624D6C6E684E6F346F4B7A54413757314578774F5975746E5A726759547166357244466238356B37516D50484D3057416B59547153755959432F71326D414E6B613042543841555A4B795556544179685341464C327442565857772F396A4C34554F306574594C696B54596535794C486858437A4568756B6A434448766D6F4B3449694D6D32753570507739654B442F564D387A312F594D6A3966787A4D396A6753435A6F6B76773D3D"));
    //bb.put(Util.toByte("74497A564939432B776A634E727144784C48574D74682F43756442764F5139304F5243514A47546B594B546B4A6446756E42634E513764492F61704F32564D314F43794B305352494E5A4747757333576D523774324C413277784D7337314B7954676470744F392F576C593D"));

    bb.put((byte)0x03);                                                 // ETX
    bb.put((byte)0x0D);                                                 // CR

    byte[] telegram = new byte[ bb.position() ];
    bb.rewind();
    bb.get( telegram );

    mRequestTelegram = new byte[telegram.length + 4];
    String telegramLength = String.format("%04d", telegram.length);
    System.arraycopy(telegramLength.getBytes(), 0, mRequestTelegram, 0, 4              );
    System.arraycopy(telegram                 , 0, mRequestTelegram, 4, telegram.length      );
  }

  private void makeTelegramReconnect() {
//       ByteBuffer bb = ByteBuffer.allocate(4096);
      ByteBuffer bb = ByteBuffer.allocate(50);

      bb.put((byte)0x02);                                                 // STX(2)
      bb.put("0007".getBytes());                                            // 전문길이(4)=  Command ID + filler + ETX + CR = 7
      bb.put("UC".getBytes());                                            // Command ID(2)
      bb.put("000".getBytes());                                           // 여유필드(3)
      bb.put((byte)0x03);                                                 // ETX(1)
      bb.put((byte)0x0D);                                                 // CR(1)

      byte[] telegram = new byte[ bb.position() ];
      bb.rewind();
      bb.get( telegram );

      mRequestTelegramReconnectKscat = new byte[telegram.length + 4];
      String telegramLength = String.format("%04d", telegram.length);
      System.arraycopy(telegramLength.getBytes(), 0, mRequestTelegramReconnectKscat, 0, 4              );
      System.arraycopy(telegram                 , 0, mRequestTelegramReconnectKscat, 4, telegram.length      );
    }

    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException
    {
        Log.d(LOG_TAG, "Action: " + action);
        if (action.equals("startActivity") || action.equals("startActivityForResult"))
        {
            //  Credit: https://github.com/chrisekelley/cordova-webintent
            if (args.length() != 1) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                return false;
            }

            JSONObject obj = args.getJSONObject(0);
            Intent intent = populateIntent(obj, callbackContext);
            int requestCode = obj.has("requestCode") ? obj.getInt("requestCode") : 1;

            boolean bExpectResult = false;
            if (action.equals("startActivityForResult"))
            {
                bExpectResult = true;
                this.onActivityResultCallbackContext = callbackContext;
            }
            startActivity(intent, bExpectResult, requestCode, callbackContext);

            return true;
        }
        else if (action.equals("sendBroadcast"))
        {
            //  Credit: https://github.com/chrisekelley/cordova-webintent
            if (args.length() != 1) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                return false;
            }

            // Parse the arguments
            JSONObject obj = args.getJSONObject(0);
            Intent intent = populateIntent(obj, callbackContext);

            sendBroadcast(intent);
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
            return true;
        }
        else if (action.equals("startService"))
        {
            if (args.length() != 1) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                return false;
            }

            // 컴포넌트 호출 서비스 시작
            JSONObject obj = args.getJSONObject(0);
            Intent intent = populateIntent(obj, callbackContext);
            startService(intent);
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
            return true;
        }
        else if (action.equals("registerBroadcastReceiver"))
        {
            Log.d(LOG_TAG, "Plugin no longer unregisters receivers on registerBroadcastReceiver invocation");

            //  No error callback
            if (args.length() != 1) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                return false;
            }

            //  Expect an array of filterActions
            JSONObject obj = args.getJSONObject(0);
            JSONArray filterActions = obj.has("filterActions") ? obj.getJSONArray("filterActions") : null;
            if (filterActions == null || filterActions.length() == 0)
            {
                //  The arguments are not correct
                Log.w(LOG_TAG, "filterActions argument is not in the expected format");
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                return false;
            }

            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);

            IntentFilter filter = new IntentFilter();
            for (int i = 0; i < filterActions.length(); i++) {
                Log.d(LOG_TAG, "Registering broadcast receiver for filter: " + filterActions.getString(i));
                filter.addAction(filterActions.getString(i));
            }

            //  Allow an array of filterCategories
            JSONArray filterCategories = obj.has("filterCategories") ? obj.getJSONArray("filterCategories") : null;
            if (filterCategories != null) {
                for (int i = 0; i < filterCategories.length(); i++) {
                    Log.d(LOG_TAG, "Registering broadcast receiver for category filter: " + filterCategories.getString(i));
                    filter.addCategory(filterCategories.getString(i));
                }
            }

            //  Add any specified Data Schemes
            //  https://github.com/darryncampbell/darryncampbell-cordova-plugin-intent/issues/24
            JSONArray filterDataSchemes = obj.has("filterDataSchemes") ? obj.getJSONArray("filterDataSchemes") : null;
            if (filterDataSchemes != null && filterDataSchemes.length() > 0)
            {
                for (int i = 0; i < filterDataSchemes.length(); i++)
                {
                    Log.d(LOG_TAG, "Associating data scheme to filter: " + filterDataSchemes.getString(i));
                    filter.addDataScheme(filterDataSchemes.getString(i));
                }
            }

            BroadcastReceiver broadcastReceiver = newBroadcastReceiver();

            this.cordova.getActivity().registerReceiver(broadcastReceiver, filter);
            receiverCallbacks.put(broadcastReceiver, callbackContext);

            callbackContext.sendPluginResult(result);
        }
        else if (action.equals("unregisterBroadcastReceiver"))
        {
            try
            {
                unregisterAllBroadcastReceivers();
            }
            catch (IllegalArgumentException e) {}
        }
        else if (action.equals("onIntent"))
        {
            //  Credit: https://github.com/napolitano/cordova-plugin-intent
            if (args.length() != 1) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                return false;
            }

            this.onNewIntentCallbackContext = callbackContext;

            if (this.deferredIntent != null) {
                fireOnNewIntent(this.deferredIntent);
                this.deferredIntent = null;
            }

            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            return true;
        }
        else if (action.equals("onActivityResult"))
        {
            if (args.length() != 1) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                return false;
            }

            this.onActivityResultCallbackContext = callbackContext;

            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            return true;
        }
        else if (action.equals("getIntent"))
        {
            //  Credit: https://github.com/napolitano/cordova-plugin-intent
            if (args.length() != 0) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                return false;
            }

            Intent intent;

            if (this.deferredIntent != null) {
                intent = this.deferredIntent;
                this.deferredIntent = null;
            }
            else {
                intent = cordova.getActivity().getIntent();
            }

            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, getIntentJson(intent)));
            return true;
        }
        else if (action.equals("sendResult"))
        {
            //  Assuming this application was started with startActivityForResult, send the result back
            //  https://github.com/darryncampbell/darryncampbell-cordova-plugin-intent/issues/3
            Intent result = new Intent();
            if (args.length() > 0) {
                JSONObject json = args.getJSONObject(0);
                JSONObject extras = (json.has("extras")) ? json.getJSONObject("extras") : null;

                // Populate the extras if any exist
                if (extras != null) {
                    JSONArray extraNames = extras.names();
                    for (int i = 0; i < extraNames.length(); i++) {
                        String key = extraNames.getString(i);
                        Object extrasObj = extras.get(key);
                        if (extrasObj instanceof JSONObject) {
                            //  The extra is a bundle
                            result.putExtra(key, toBundle((JSONObject) extras.get(key)));
                        } else if (extrasObj instanceof Boolean) {
                            result.putExtra(key, extras.getBoolean(key));
                        } else if (extrasObj instanceof Integer) {
                            result.putExtra(key, extras.getInt(key));
                        } else if (extrasObj instanceof Long) {
                            result.putExtra(key, extras.getLong(key));
                        } else if (extrasObj instanceof Double) {
                            result.putExtra(key, extras.getDouble(key));
                        } else if (extrasObj instanceof Float) {
                            result.putExtra(key, extras.getDouble(key));
                        } else {
                            result.putExtra(key, extras.getString(key));
                        }
                    }
                }
            }

            //set result
            cordova.getActivity().setResult(Activity.RESULT_OK, result);
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));

            //finish the activity
            cordova.getActivity().finish();

        }
        else if (action.equals("realPathFromUri"))
        {
            if (args.length() != 1) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                return false;
            }

            JSONObject obj = args.getJSONObject(0);
            String realPath = getRealPathFromURI_API19(obj, callbackContext);
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, realPath));
            return true;

        }
        else if (action.equals("packageExists"))
        {
            try {
                if (args.length() < 1) {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                    return false;
                }

                PackageManager packageManager = this.cordova.getActivity().getApplicationContext().getPackageManager();
                packageManager.getPackageInfo(args.getString(0), 0);
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, true));
                return true;
            } catch (PackageManager.NameNotFoundException e) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, false));
                return true;
            }
        }

        return true;
    }

    private void unregisterAllBroadcastReceivers() {
        Log.d(LOG_TAG, "Unregistering all broadcast receivers, size was " + receiverCallbacks.size());
        for (BroadcastReceiver broadcastReceiver: receiverCallbacks.keySet()){
            this.cordova.getActivity().unregisterReceiver(broadcastReceiver);
        }
        receiverCallbacks.clear();
    }

    private Uri remapUriWithFileProvider(String uriAsString, final CallbackContext callbackContext)
    {
        //  Create the URI via FileProvider  Special case for N and above when installing apks
        int permissionCheck = ContextCompat.checkSelfPermission(this.cordova.getActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED)
        {
            //  Could do better here - if the app does not already have permission should
            //  only continue when we get the success callback from this.
            ActivityCompat.requestPermissions(this.cordova.getActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            callbackContext.error("Please grant read external storage permission");
            return null;
        }

        try
        {
            String externalStorageState = getExternalStorageState();
            if (externalStorageState.equals(Environment.MEDIA_MOUNTED) || externalStorageState.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
                String fileName = uriAsString.substring(uriAsString.indexOf('/') + 2, uriAsString.length());
                File uriAsFile = new File(fileName);
                boolean fileExists = uriAsFile.exists();
                if (!fileExists)
                {
                    Log.e(LOG_TAG, "File at path " + uriAsFile.getPath() + " with name " + uriAsFile.getName() + "does not exist");
                    callbackContext.error("File not found: " + uriAsFile.toString());
                    return null;
                }
                String PACKAGE_NAME = this.cordova.getActivity().getPackageName() + ".darryncampbell.cordova.plugin.intent.fileprovider";
                Uri uri = FileProvider.getUriForFile(this.cordova.getActivity().getApplicationContext(), PACKAGE_NAME, uriAsFile);
                return uri;
            }
            else
            {
                Log.e(LOG_TAG, "Storage directory is not mounted.  Please ensure the device is not connected via USB for file transfer");
                callbackContext.error("Storage directory is returning not mounted");
                return null;
            }
        } catch (StringIndexOutOfBoundsException e)
        {
            Log.e(LOG_TAG, "URL is not well formed");
            callbackContext.error("URL is not well formed");
            return null;
        }
    }

    private String getRealPathFromURI_API19(JSONObject obj, CallbackContext callbackContext) throws JSONException
    {
        //  Credit: https://stackoverflow.com/questions/2789276/android-get-real-path-by-uri-getpath/2790688
        Uri uri = obj.has("uri") ? Uri.parse(obj.getString("uri")) : null;
        if (uri == null)
        {
            Log.w(LOG_TAG, "URI is not a specified parameter");
            throw new JSONException("URI is not a specified parameter");
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            String filePath = "";
            if (uri.getHost().contains("com.android.providers.media")) {
                int permissionCheck = ContextCompat.checkSelfPermission(this.cordova.getActivity(),
                        Manifest.permission.READ_EXTERNAL_STORAGE);
                if (permissionCheck != PackageManager.PERMISSION_GRANTED)
                {
                    //  Could do better here - if the app does not already have permission should
                    //  only continue when we get the success callback from this.
                    ActivityCompat.requestPermissions(this.cordova.getActivity(),
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                    callbackContext.error("Please grant read external storage permission");
                    return null;
                }

                // Image pick from recent
                String wholeID = DocumentsContract.getDocumentId(uri);

                // Split at colon, use second item in the array
                String id = wholeID.split(":")[1];

                String[] column = {MediaStore.Images.Media.DATA};

                // where id is equal to
                String sel = MediaStore.Images.Media._ID + "=?";

                //  This line requires read storage permission

                Cursor cursor = this.cordova.getActivity().getApplicationContext().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        column, sel, new String[]{id}, null);

                int columnIndex = cursor.getColumnIndex(column[0]);

                if (cursor.moveToFirst()) {
                    filePath = cursor.getString(columnIndex);
                }
                cursor.close();
                return filePath;
            } else {
                // image pick from gallery
                String[] proj = {MediaStore.Images.Media.DATA};
                Cursor cursor = this.cordova.getActivity().getApplicationContext().getContentResolver().query(uri, proj, null, null, null);
                int column_index
                        = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(column_index);
            }
        }

        return "Requires KK or higher";
    }

    private void startActivity(Intent i, boolean bExpectResult, int requestCode, CallbackContext callbackContext) {

        if (i.resolveActivityInfo(this.cordova.getActivity().getPackageManager(), 0) != null)
        {
            if (bExpectResult)
            {
                cordova.setActivityResultCallback(this);
                this.cordova.getActivity().startActivityForResult(i, requestCode);
            }
            else
            {
                this.cordova.getActivity().startActivity(i);
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
            }
        }
        else
        {
            //  Return an error as there is no app to handle this intent
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR));
        }
    }

    private void sendBroadcast(Intent intent) {
        this.cordova.getActivity().sendBroadcast(intent);
    }

    private void startService(Intent intent)
    {
        this.cordova.getActivity().startService(intent);
    }

    private Intent populateKsnetIntent(JSONObject obj, CallbackContext callbackContext) throws JSONException
    {
      HashMap<String,String> hashMap = new HashMap<>();
      Intent intent = null;
      Log.d(LOG_TAG, "kalen populateKsnetIntent !!!");
      Log.d(LOG_TAG, "kalen populateKsnetIntent "+obj.toString());


        Log.d(LOG_TAG, "kalen 결제");
        makeTelegramIC(obj.getString("tid"),
            obj.getString("installment"),
            obj.getString("totalAmount"),
            obj.getString("tax"),
            obj.getString("amount"),
            obj.getString("sign"));
        // makeTelegramVANTR();
        ComponentName componentName = new ComponentName("com.ksnet.kscat_a","com.ksnet.kscat_a.PaymentIntentActivity");
        intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(componentName);
        intent.putExtra("Telegram", mRequestTelegram);
        intent.putExtra("TelegramLength", mRequestTelegram.length);

      Log.d(LOG_TAG, "kalen return intent ...");
      return intent;
      }

    private Intent populateReconnectKscatIntent(JSONObject obj, CallbackContext callbackContext) throws JSONException
    {
        HashMap<String,String> hashMap = new HashMap<>();
        Intent intent = null;
        Log.d(LOG_TAG, "kalen populateReconnectKscatIntent !!!");
        Log.d(LOG_TAG, "kalen populateReconnectKscatIntent "+obj.toString());


        Log.d(LOG_TAG, "kalen reconnect_kscat");

        // mRequestTelegramReconnectKscat 가공
        makeTelegramReconnect();
        // makeTelegramVANTR();
        ComponentName componentName = new ComponentName("com.ksnet.kscat_a","com.ksnet.kscat_a.PaymentIntentActivity");
        intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(componentName);
        intent.putExtra("Telegram", mRequestTelegramReconnectKscat);
        intent.putExtra("TelegramLength", mRequestTelegramReconnectKscat.length);

        Log.d(LOG_TAG, "kalen return intent ...");
        return intent;
    }

    private Intent populateIntent(JSONObject obj, CallbackContext callbackContext) throws JSONException
    {
        // payment 결제
        if(obj.has("package") && obj.getString("package").equals("com.elb.payment")){
            return this.populateKsnetIntent(obj,callbackContext);
        }

        // 단말기 재연결
        if(obj.has("package") && obj.getString("package").equals("com.elb.payment.reconnect_kscat")){
            return this.populateReconnectKscatIntent(obj,callbackContext);
        }

        //  Credit: https://github.com/chrisekelley/cordova-webintent
        String type = obj.has("type") ? obj.getString("type") : null;
        String packageAssociated = obj.has("package") ? obj.getString("package") : null;

        //Uri uri = obj.has("url") ? resourceApi.remapUri(Uri.parse(obj.getString("url"))) : null;
        Uri uri = null;
        final CordovaResourceApi resourceApi = webView.getResourceApi();
        if (obj.has("url"))
        {
            String uriAsString = obj.getString("url");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && uriAsString.startsWith("file://"))
            {
                uri = remapUriWithFileProvider(uriAsString, callbackContext);
            }
            else
            {
                uri = resourceApi.remapUri(Uri.parse(obj.getString("url")));
            }
        }

        JSONObject extras = obj.has("extras") ? obj.getJSONObject("extras") : null;
        Map<String, Object> extrasMap = new HashMap<String, Object>();
        JSONObject extrasObject = null;
        String extrasKey = "";
        if (extras != null) {
            JSONArray extraNames = extras.names();
            for (int i = 0; i < extraNames.length(); i++) {
                String key = extraNames.getString(i);
                Object extrasObj = extras.get(key);
                if (extrasObj instanceof JSONObject) {
                    //  The extra is a bundle
                    extrasKey = key;
                    extrasObject = (JSONObject) extras.get(key);
                } else {
                    extrasMap.put(key, extras.get(key));
                }
            }
        }

        String action = obj.has("action") ? obj.getString("action") : null;
        Intent i = new Intent();
        if (action != null)
            i.setAction(action);

        if (type != null && uri != null) {
            i.setDataAndType(uri, type); //Fix the crash problem with android 2.3.6
        } else {
            if (type != null) {
                i.setType(type);
            }
            if (uri != null)
            {
                i.setData(uri);
            }
        }

        JSONObject component = obj.has("component") ? obj.getJSONObject("component") : null;
        if (component != null)
        {
            //  User has specified an explicit intent
            String componentPackage = component.has("package") ? component.getString("package") : null;
            String componentClass = component.has("class") ? component.getString("class") : null;
            if (componentPackage == null || componentClass == null)
            {
                Log.w(LOG_TAG, "Component specified but missing corresponding package or class");
                throw new JSONException("Component specified but missing corresponding package or class");
            }
            else
            {
                ComponentName componentName = new ComponentName(componentPackage, componentClass);
                i.setComponent(componentName);
            }
        }

        if (packageAssociated != null)
            i.setPackage(packageAssociated);

        JSONArray flags = obj.has("flags") ? obj.getJSONArray("flags") : null;
        if (flags != null)
        {
            int length = flags.length();
            for (int k = 0; k < length; k++)
            {
                i.addFlags(flags.getInt(k));
            }
        }

        if (extrasObject != null)
            addSerializable(i, extrasKey, extrasObject);

        for (String key : extrasMap.keySet()) {
            Object value = extrasMap.get(key);
            String valueStr = String.valueOf(value);
            // If type is text html, the extra text must sent as HTML
            if (key.equals(Intent.EXTRA_TEXT) && type.equals("text/html")) {
                i.putExtra(key, Html.fromHtml(valueStr));
            } else if (key.equals(Intent.EXTRA_STREAM)) {
                // allows sharing of images as attachments.
                // value in this case should be a URI of a file
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && valueStr.startsWith("file://"))
                {
                    Uri uriOfStream = remapUriWithFileProvider(valueStr, callbackContext);
                    if (uriOfStream != null)
                        i.putExtra(key, uriOfStream);
                }
                else
                {
                    //final CordovaResourceApi resourceApi = webView.getResourceApi();
                    i.putExtra(key, resourceApi.remapUri(Uri.parse(valueStr)));
                }
            } else if (key.equals(Intent.EXTRA_EMAIL)) {
                // allows to add the email address of the receiver
                i.putExtra(Intent.EXTRA_EMAIL, new String[] { valueStr });
            } else if (key.equals(Intent.EXTRA_KEY_EVENT)) {
                // allows to add a key event object
                JSONObject keyEventJson = new JSONObject(valueStr);
                int keyAction = keyEventJson.getInt("action");
                int keyCode = keyEventJson.getInt("code");
                KeyEvent keyEvent = new KeyEvent(keyAction, keyCode);
                i.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
            } else {
                if (value instanceof Boolean) {
                    i.putExtra(key, Boolean.valueOf(valueStr));
                } else if (value instanceof Integer) {
                    i.putExtra(key, Integer.valueOf(valueStr));
                } else if (value instanceof Long) {
                    i.putExtra(key, Long.valueOf(valueStr));
                } else if (value instanceof Double) {
                    i.putExtra(key, Double.valueOf(valueStr));
                } else {
                    i.putExtra(key, valueStr);
                }
            }
        }

        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (obj.has("chooser")) {
            i = Intent.createChooser(i, obj.getString("chooser"));
        }

        return i;
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (this.onNewIntentCallbackContext != null) {
            fireOnNewIntent(intent);
        } else {
            // save the intent for use when onIntent action is called in the execute method
            this.deferredIntent = intent;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        super.onActivityResult(requestCode, resultCode, intent);

        if (onActivityResultCallbackContext != null && intent != null)
        {
            // kscat 재연결 요청에 대한 KSCAT_A 의 응답
            if(requestCode == 100) {
              byte[] recvByte = intent.getByteArrayExtra("responseTelegram");
              ExtraReconnectKscat trData = new ExtraReconnectKscat();
              trData.SetData(recvByte);

              intent.putExtra("transactionCode", new String(trData.transactionCode));
              intent.putExtra("resultCode", resultCode);
              PluginResult result = new PluginResult(PluginResult.Status.OK, getIntentJson(intent));
              result.setKeepCallback(true);
              onActivityResultCallbackContext.sendPluginResult(result);
            }

            // 결제 응답
            else if((resultCode == RESULT_OK || resultCode == RESULT_CANCELED) && intent != null && intent.hasExtra("responseTelegram"))
            {
              byte[] recvByte = intent.getByteArrayExtra("responseTelegram");
              TransactionData trData = new TransactionData();
              trData.SetData(recvByte);

//              if(trData.transactionCode != null && !isEmpty(trData.transactionCode)){
              intent.putExtra("transactionCode", new String(trData.transactionCode));
//              }
//              if(trData.operationCode != null && !isEmpty(trData.operationCode)){
              intent.putExtra("operationCode", new String(trData.operationCode));
//               }
//              if(trData.transferCode != null && !isEmpty(trData.transferCode)){
              intent.putExtra("transferCode", new String(trData.transferCode));
//               }
//              if(trData.transferType != null && !isEmpty(trData.transferType)){
              intent.putExtra("transferType", new String(trData.transferType));
//               }
//              if(trData.deviceNumber != null && !isEmpty(trData.deviceNumber)){
              intent.putExtra("deviceNumber", new String(trData.deviceNumber));
//               }
//              if(trData.companyInfo != null && !isEmpty(trData.companyInfo)){
              intent.putExtra("companyInfo", new String(trData.companyInfo));
//               }
//              if(trData.transferSerialNumber != null && !isEmpty(trData.transferSerialNumber)){
              intent.putExtra("transferSerialNumber", new String(trData.transferSerialNumber));
//               }
//              if(trData.status != null && !isEmpty(trData.status)){
              intent.putExtra("status", new String(trData.status));
//               }
//              if(trData.standardCode != null && !isEmpty(trData.standardCode)){
              intent.putExtra("standardCode", new String(trData.standardCode));
//               }
//              if(trData.cardCompanyCode != null && !isEmpty(trData.cardCompanyCode)){
              intent.putExtra("cardCompanyCode", new String(trData.cardCompanyCode));
//               }
//              if(trData.transferDate != null && !isEmpty(trData.transferDate)){
              intent.putExtra("transferDate", new String(trData.transferDate));
//               }
//              if(trData.cardType != null && !isEmpty(trData.cardType)){
              intent.putExtra("cardType", new String(trData.cardType));
//               }
//              if(trData.approvalNumber != null && !isEmpty(trData.approvalNumber)){
              intent.putExtra("approvalNumber", new String(trData.approvalNumber));
//               }
//              if(trData.transactionUniqueNumber != null && !isEmpty(trData.transactionUniqueNumber)){
              intent.putExtra("transactionUniqueNumber", new String(trData.transactionUniqueNumber));
//               }
//              if(trData.merchantNumber != null && !isEmpty(trData.merchantNumber)){
              intent.putExtra("merchantNumber", new String(trData.merchantNumber));
//               }
//              if(trData.IssuanceCode != null && !isEmpty(trData.IssuanceCode)){
              intent.putExtra("IssuanceCode", new String(trData.IssuanceCode));
//               }
//              if(trData.purchaseCompanyCode != null && !isEmpty(trData.purchaseCompanyCode)){
              intent.putExtra("purchaseCompanyCode", new String(trData.purchaseCompanyCode));
//               }
//              if(trData.workingKeyIndex != null && !isEmpty(trData.workingKeyIndex)){
              intent.putExtra("workingKeyIndex", new String(trData.workingKeyIndex));
//               }
//              if(trData.workingKey != null && !isEmpty(trData.workingKey)){
              intent.putExtra("workingKey", new String(trData.workingKey));
//               }
//              if(trData.balance != null && !isEmpty(trData.balance)){
              intent.putExtra("balance", new String(trData.balance));
//               }
//              if(trData.point1 != null && !isEmpty(trData.point1)){
              intent.putExtra("point1", new String(trData.point1));
//               }
//              if(trData.point2 != null && !isEmpty(trData.point2)){
              intent.putExtra("point2", new String(trData.point2));
//               }
//              if(trData.point3 != null && !isEmpty(trData.point3)){
              intent.putExtra("point3", new String(trData.point3));
//               }
              try {
//              if(trData.purchaseCompanyName != null && !isEmpty(trData.purchaseCompanyName)){
                intent.putExtra("purchaseCompanyName", new String(trData.purchaseCompanyName,"EUC-KR"));
//               }
//              if(trData.cardCategoryName != null && !isEmpty(trData.cardCategoryName)){
                intent.putExtra("cardCategoryName", new String(trData.cardCategoryName,"EUC-KR"));
//               }
//              if(trData.message1 != null && !isEmpty(trData.message1)){
                intent.putExtra("message1", new String(trData.message1,"EUC-KR"));
//               }
//              if(trData.message2 != null && !isEmpty(trData.message2)){
                intent.putExtra("message2", new String(trData.message2,"EUC-KR"));
//               }
//              if(trData.notice2 != null && !isEmpty(trData.notice2)){
                intent.putExtra("notice2", new String(trData.notice2,"EUC-KR"));
//               }
//              if(trData.notice1 != null && !isEmpty(trData.notice1)){
                intent.putExtra("notice1", new String(trData.notice1, "EUC-KR"));
//               }
              } catch (UnsupportedEncodingException e) {
//                 throw new RuntimeException(e);
              }
//              if(trData.reserved != null && !isEmpty(trData.reserved)){
              intent.putExtra("reserved", new String(trData.reserved));
//               }
//              if(trData.KSNETreserved != null && !isEmpty(trData.KSNETreserved)){
              intent.putExtra("KSNETreserved", new String(trData.KSNETreserved));
//               }
//              if(trData.filler != null && !isEmpty(trData.filler)){
              intent.putExtra("filler", new String(trData.filler));
//               }

              intent.putExtra("resultCode", resultCode);
              PluginResult result = new PluginResult(PluginResult.Status.OK, getIntentJson(intent));
              result.setKeepCallback(true);
              onActivityResultCallbackContext.sendPluginResult(result);
            }
            else
            {
            intent.putExtra("requestCode", requestCode);
            intent.putExtra("resultCode", resultCode);
            PluginResult result = new PluginResult(PluginResult.Status.OK, getIntentJson(intent));
            result.setKeepCallback(true);
            onActivityResultCallbackContext.sendPluginResult(result);
            }
        }
        else if (onActivityResultCallbackContext != null)
        {
            Intent canceledIntent = new Intent();
            canceledIntent.putExtra("requestCode", requestCode);
            canceledIntent.putExtra("resultCode", resultCode);
            PluginResult canceledResult = new PluginResult(PluginResult.Status.OK, getIntentJson(canceledIntent));
            canceledResult.setKeepCallback(true);
            onActivityResultCallbackContext.sendPluginResult(canceledResult);
        }
    }

    private BroadcastReceiver newBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                CallbackContext onBroadcastCallbackContext = receiverCallbacks.get(this);
                if (onBroadcastCallbackContext != null)
                {
                    PluginResult result = new PluginResult(PluginResult.Status.OK, getIntentJson(intent));
                    result.setKeepCallback(true);
                    onBroadcastCallbackContext.sendPluginResult(result);
                }
            }
        };
    }

    /**
     * Sends the provided Intent to the onNewIntentCallbackContext.
     *
     * @param intent This is the intent to send to the JS layer.
     */
    private void fireOnNewIntent(Intent intent) {
        PluginResult result = new PluginResult(PluginResult.Status.OK, getIntentJson(intent));
        result.setKeepCallback(true);
        this.onNewIntentCallbackContext.sendPluginResult(result);
    }

    /**
     * Return JSON representation of intent attributes
     *
     * @param intent
     * Credit: https://github.com/napolitano/cordova-plugin-intent
     */
    private JSONObject getIntentJson(Intent intent) {
        JSONObject intentJSON = null;
        ClipData clipData = null;
        JSONObject[] items = null;
        ContentResolver cR = this.cordova.getActivity().getApplicationContext().getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            clipData = intent.getClipData();
            if (clipData != null) {
                int clipItemCount = clipData.getItemCount();
                items = new JSONObject[clipItemCount];

                for (int i = 0; i < clipItemCount; i++) {

                    ClipData.Item item = clipData.getItemAt(i);

                    try {
                        items[i] = new JSONObject();
                        items[i].put("htmlText", item.getHtmlText());
                        items[i].put("intent", item.getIntent());
                        items[i].put("text", item.getText());
                        items[i].put("uri", item.getUri());

                        if (item.getUri() != null) {
                            String type = cR.getType(item.getUri());
                            String extension = mime.getExtensionFromMimeType(cR.getType(item.getUri()));

                            items[i].put("type", type);
                            items[i].put("extension", extension);
                        }

                    } catch (JSONException e) {
                        Log.d(LOG_TAG, " Error thrown during intent > JSON conversion");
                        Log.d(LOG_TAG, e.getMessage());
                        Log.d(LOG_TAG, Arrays.toString(e.getStackTrace()));
                    }

                }
            }
        }

        try {
            intentJSON = new JSONObject();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (items != null) {
                    intentJSON.put("clipItems", new JSONArray(items));
                }
            }

            intentJSON.put("type", intent.getType());
            intentJSON.put("extras", toJsonObject(intent.getExtras()));
            intentJSON.put("action", intent.getAction());
            intentJSON.put("categories", intent.getCategories());
            intentJSON.put("flags", intent.getFlags());
            intentJSON.put("component", intent.getComponent());
            intentJSON.put("data", intent.getData());
            intentJSON.put("package", intent.getPackage());

            return intentJSON;
        } catch (JSONException e) {
            Log.d(LOG_TAG, " Error thrown during intent > JSON conversion");
            Log.d(LOG_TAG, e.getMessage());
            Log.d(LOG_TAG, Arrays.toString(e.getStackTrace()));

            return null;
        }
    }

    private static JSONObject toJsonObject(Bundle bundle) {
        //  Credit: https://github.com/napolitano/cordova-plugin-intent
        try {
            return (JSONObject) toJsonValue(bundle);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Cannot convert bundle to JSON: " + e.getMessage(), e);
        }
    }

    private static Object toJsonValue(final Object value) throws JSONException {
        //  Credit: https://github.com/napolitano/cordova-plugin-intent
        if (value == null) {
            return null;
        } else if (value instanceof Bundle) {
            final Bundle bundle = (Bundle) value;
            final JSONObject result = new JSONObject();
            for (final String key : bundle.keySet()) {
                result.put(key, toJsonValue(bundle.get(key)));
            }
            return result;
        } else if ((value.getClass().isArray())) {
            final JSONArray result = new JSONArray();
            int length = Array.getLength(value);
            for (int i = 0; i < length; ++i) {
                result.put(i, toJsonValue(Array.get(value, i)));
            }
            return result;
        }else if (value instanceof ArrayList<?>) {
            final ArrayList arrayList = (ArrayList<?>)value;
            final JSONArray result = new JSONArray();
            for (int i = 0; i < arrayList.size(); i++)
                result.put(toJsonValue(arrayList.get(i)));
            return result;
        } else if (
                value instanceof String
                        || value instanceof Boolean
                        || value instanceof Integer
                        || value instanceof Long
                        || value instanceof Double) {
            return value;
        } else {
            return String.valueOf(value);
        }
    }

    private void addSerializable(Intent intent, String key, final JSONObject obj) {
        if (obj.has("$class")) {
            try {
                JSONArray arguments = obj.has("arguments") ? obj.getJSONArray("arguments") : new JSONArray();

                Class<?>[] argTypes = new Class[arguments.length()];
                for (int i = 0; i < arguments.length(); i++) {
                    argTypes[i] = getType(arguments.get(i));
                }

                Class<?> classForName = Class.forName(obj.getString("$class"));
                Constructor<?> constructor = classForName.getConstructor(argTypes);

                intent.putExtra(key, (Serializable) constructor.newInstance(jsonArrayToObjectArray(arguments)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            intent.putExtra(key, toBundle(obj));
        }
    }

    private Object[] jsonArrayToObjectArray(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {
            list.add(array.get(i));
        }

        return list.toArray();
    }

    private Class<?> getType(Object obj) {
        if (obj instanceof String) {
            return String.class;
        } else if (obj instanceof Boolean) {
            return Boolean.class;
        } else if (obj instanceof Float) {
            return Float.class;
        } else if (obj instanceof Integer) {
            return Integer.class;
        } else if (obj instanceof Long) {
            return Long.class;
        } else if (obj instanceof Double) {
            return Double.class;
        } else {
            return null;
        }
    }

    private Bundle toBundle(final JSONObject obj) {
        Bundle returnBundle = new Bundle();
        if (obj == null) {
            return null;
        }
        try {
            Iterator<?> keys = obj.keys();
            while (keys.hasNext()) {
                String key = (String)keys.next();

                if (obj.get(key) instanceof String)
                    returnBundle.putString(key, obj.getString(key));
                else if (obj.get(key) instanceof Boolean)
                    returnBundle.putBoolean(key, obj.getBoolean(key));
                else if (obj.get(key) instanceof Integer)
                    returnBundle.putInt(key, obj.getInt(key));
                else if (obj.get(key) instanceof Long)
                    returnBundle.putLong(key, obj.getLong(key));
                else if (obj.get(key) instanceof Double)
                    returnBundle.putDouble(key, obj.getDouble(key));
                else if (obj.get(key).getClass().isArray() || obj.get(key) instanceof JSONArray)
                {
                    JSONArray jsonArray = obj.getJSONArray(key);
                    int length = jsonArray.length();
                    if (jsonArray.get(0) instanceof String)
                    {
                        String[] stringArray = new String[length];
                        for (int j = 0; j < length; j++)
                            stringArray[j] = jsonArray.getString(j);
                        returnBundle.putStringArray(key, stringArray);
                        //returnBundle.putParcelableArray(key, obj.get);
                    }
                    else
                    {
                        if (key.equals("PLUGIN_CONFIG")) {
                            ArrayList<Bundle> bundleArray = new ArrayList<Bundle>();
                            for (int k = 0; k < length; k++) {
                                bundleArray.add(toBundle(jsonArray.getJSONObject(k)));
                            }
                            returnBundle.putParcelableArrayList(key, bundleArray);
                        } else {
                            Bundle[] bundleArray = new Bundle[length];
                            for (int k = 0; k < length; k++)
                                bundleArray[k] = toBundle(jsonArray.getJSONObject(k));
                            returnBundle.putParcelableArray(key, bundleArray);
                        }
                    }
                }
                else if (obj.get(key) instanceof JSONObject)
                    returnBundle.putBundle(key, toBundle((JSONObject)obj.get(key)));
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        return returnBundle;
    }
}
