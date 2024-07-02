package com.example.sca.ui.cloud.primary;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.common.OSSLog;
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider;
import com.alibaba.sdk.android.oss.model.GetObjectRequest;
import com.alibaba.sdk.android.oss.model.GetObjectResult;
import com.example.sca.Config;
import com.example.sca.R;
import com.example.sca.ui.cloud.backup.OssBackupSync;
import com.example.sca.ui.cloud.primary.bucket.BucketsAdapter;
import com.example.sca.ui.cloud.primary.common.base.BaseActivity;
import com.example.sca.ui.cloud.utils.CosServiceFactory;
import com.example.sca.ui.cloud.utils.CosUserInformation;
import com.example.sca.ui.cloud.utils.FileHashUtil;
import com.tencent.cos.xml.CosXmlService;
import com.tencent.cos.xml.exception.CosXmlClientException;
import com.tencent.cos.xml.exception.CosXmlServiceException;
import com.tencent.cos.xml.listener.CosXmlResultListener;
import com.tencent.cos.xml.model.CosXmlRequest;
import com.tencent.cos.xml.model.CosXmlResult;
import com.tencent.cos.xml.model.service.GetServiceRequest;
import com.tencent.cos.xml.model.service.GetServiceResult;
import com.tencent.cos.xml.model.tag.ListAllMyBuckets;
import com.tencent.cos.xml.transfer.COSXMLDownloadTask;
import com.tencent.cos.xml.transfer.TransferConfig;
import com.tencent.cos.xml.transfer.TransferManager;
import com.tencent.cos.xml.transfer.TransferState;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AuditActivity extends BaseActivity {
    private static final String TAG = "AuditActivity";
    private CosUserInformation cosUserInformation;
    private BucketsAdapter adapter;

    private String bucketName;
    private String bucketRegion;

    private ArrayList<String> list = new ArrayList<String>();

    /*
     * {@link TransferManager} 进一步封装了 {@link CosXmlService} 的上传和下载接口，当您需要
     * 上传文件到 COS 或者从 COS 下载文件时，请优先使用这个类。
     */
    private TransferManager transferManager;
    private ListView listView;

    private OSSPlainTextAKSKCredentialProvider credentialProvider;
    private OSS ossClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_cloud);
        listView = findViewById(R.id.listview);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ListAllMyBuckets.Bucket bucket = adapter.getItem(i);
                if (bucket != null) {
                    bucketName = bucket.name;
                    bucketRegion = bucket.location;
                    showAuditDialog();

                }

            }
        });

        // cos
        cosUserInformation = new CosUserInformation(Config.COS_SECRET_ID, Config.COS_SECRET_KEY, Config.COS_APP_ID);

        //oss
        credentialProvider = new OSSPlainTextAKSKCredentialProvider(Config.OSS_ACCESS_KEY_ID, Config.OSS_ACCESS_KEY_SECRET);


        if (cosUserInformation.getCOS_SECRET_ID().length() == 0 || cosUserInformation.getCOS_SECRET_KEY().length() == 0 ||
                credentialProvider.getAccessKeyId().length() == 0 || credentialProvider.getAccessKeySecret().length() == 0) {
            Toast.makeText(this, "请在配置文件中配置您的secretId和secretKey", Toast.LENGTH_SHORT).show();
        } else {
            // cos
            CosXmlService cosXmlService = CosServiceFactory.getCosXmlService(this,
                    cosUserInformation.getCOS_SECRET_ID(), cosUserInformation.getCOS_SECRET_KEY(), false);

            // oss
            ossClient = new OSSClient(this, Config.OSS_ENDPOINT, credentialProvider); // 备份云存储桶地域固定为 endpoint 中的地域


            getBuckets(cosXmlService);// 查询存储桶列表
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.synchronization, menu);
        menu.findItem(R.id.sync).setVisible(true);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.sync) {
            Intent intent = new Intent(this, OssBackupSync.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);

    }

    // 查询存储桶列表
    private void getBuckets(CosXmlService cosXmlService) {
        showLoadingDialog();

        cosXmlService.getServiceAsync(new GetServiceRequest(), new CosXmlResultListener() {
            @Override
            public void onSuccess(CosXmlRequest request, final CosXmlResult result) {
                uiAction(new Runnable() {
                    @Override
                    public void run() {
                        dismissLoadingDialog();
                        List<ListAllMyBuckets.Bucket> buckets = ((GetServiceResult) result).listAllMyBuckets.buckets;
                        if (adapter == null) {
                            adapter = new BucketsAdapter(buckets, AuditActivity.this);
                            listView.setAdapter(adapter);
                        } else {
                            adapter.setDataList(buckets);
                        }
                    }
                });
            }

            @Override
            public void onFail(CosXmlRequest request, CosXmlClientException exception, CosXmlServiceException serviceException) {
                dismissLoadingDialog();

                Toast.makeText(AuditActivity.this, "获取存储桶列表失败", Toast.LENGTH_SHORT).show();
                if (exception != null) {
                    exception.printStackTrace();
                }
                if (serviceException != null) {
                    serviceException.printStackTrace();
                }
            }
        });
    }


    // 带“是”和“否”的提示框
    private void showAuditDialog() {
        CosXmlService cosXmlServicedownload = CosServiceFactory.getCosXmlService(this, bucketRegion,
                cosUserInformation.getCOS_SECRET_ID(), cosUserInformation.getCOS_SECRET_KEY(), true);
        TransferConfig transferConfig = new TransferConfig.Builder().build();
        transferManager = new TransferManager(cosXmlServicedownload, transferConfig);

        File file = new File(getFilesDir(), bucketName + "filedir.txt");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            Log.d(TAG, "addfilelistitem: ");
            if (file.length() != 0) {
                FileInputStream inputStream = new FileInputStream(file);
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                list = (ArrayList<String>) objectInputStream.readObject();
                objectInputStream.close();
                inputStream.close();
            }
            Log.d(TAG, "list: " + list);
            if (list.isEmpty()) {
                toastMessage("无可审计文件");
                return;
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        new AlertDialog.Builder(this)
                .setTitle("审计 " + bucketName + " 存储桶")
                .setMessage("请选择审计方式")
                .setPositiveButton("简易审计", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int round = Math.min(list.size(), 20); // 超过20个就只审计20个，不足20个则全部审计
                        ArrayList<String> subList = (ArrayList<String>) RandomLists.newRandomList(list, round);
                        Log.d(TAG, "subList: "+subList);
                        for(String path : subList) {
                            cosdownload(path);
                        }


                    }
                })
                .setNegativeButton("全部审计", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // 遍历 list 中 全部路径
                        for(String path : list) {
                            cosdownload(path);
                        }
                    }
                })
                .show();
    }

    private void ossdownload(String path) {
        String downloadPath = Environment.getExternalStorageDirectory()
                + File.separator + Environment.DIRECTORY_DCIM
                + File.separator + "cosdownload" + File.separator;
        String filename = FileHashUtil.getFileNameWithSuffix(path);
        // 构造下载文件请求。
        GetObjectRequest get = new GetObjectRequest(bucketName, path);

        ossClient.asyncGetObject(get, new OSSCompletedCallback<GetObjectRequest, GetObjectResult>() {
            @Override
            public void onSuccess(GetObjectRequest request, GetObjectResult result) {
                // 开始读取数据。
                long length = result.getContentLength();
                if (length > 0) {
                    byte[] buffer = new byte[(int) length];
                    int readCount = 0;
                    while (readCount < length) {
                        try {
                            readCount += result.getObjectContent().read(buffer, readCount, (int) length - readCount);
                        } catch (Exception e) {
                            OSSLog.logInfo(e.toString());
                        }
                    }
                    // 将下载后的文件存放在指定的本地路径，例如D:\\localpath\\exampleobject.jpg。
                    try {
                        FileOutputStream fout = new FileOutputStream(downloadPath + "oss_" + filename);
                        fout.write(buffer);
                        fout.close();
                    } catch (Exception e) {
                        OSSLog.logInfo(e.toString());
                    }
                }
                osscheckFileHash(path);

            }

            @Override
            public void onFailure(GetObjectRequest request, ClientException clientException,
                                  ServiceException serviceException) {
                // 请求异常。
                if (clientException != null) {
                    // 本地异常，如网络异常等。
                    clientException.printStackTrace();
                }
                if (serviceException != null) {
                    // 服务异常。
                    Log.e("ErrorCode", serviceException.getErrorCode());
                    Log.e("RequestId", serviceException.getRequestId());
                    Log.e("HostId", serviceException.getHostId());
                    Log.e("RawMessage", serviceException.getRawMessage());
                }

            }
        });

    }


    private void cosdownload(String path) {
        String downloadPath = Environment.getExternalStorageDirectory()
                + File.separator + Environment.DIRECTORY_DCIM
                + File.separator + "cosdownload" + File.separator;
        String filename = FileHashUtil.getFileNameWithSuffix(path);
        COSXMLDownloadTask cosxmlTask = transferManager.download(this, bucketName, path,
                downloadPath, filename);

        cosxmlTask.setCosXmlResultListener(new CosXmlResultListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onSuccess(CosXmlRequest request, CosXmlResult result) {
                downloadFileHash(path);
            }

            @Override
            public void onFail(CosXmlRequest request, CosXmlClientException exception, CosXmlServiceException serviceException) {
                if (cosxmlTask.getTaskState() != TransferState.PAUSED) {
                    toastMessage("审计失败");
                }
                if (exception != null) {
                    exception.printStackTrace();
                }
                if (serviceException != null) {
                    serviceException.printStackTrace();
                }
            }
        });

    }

    private void downloadFileHash(String path) {
        String name = FileHashUtil.getFileName(path) + ".txt";
        String cospath = "filehash" + File.separator + name;
        COSXMLDownloadTask cosxmlTask = transferManager.download(this, bucketName, cospath,
                String.valueOf(getFilesDir()), name);


        cosxmlTask.setCosXmlResultListener(new CosXmlResultListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onSuccess(CosXmlRequest request, CosXmlResult result) {
                coscheckFileHash(path);
                ossdownload(path);
            }

            @Override
            public void onFail(CosXmlRequest request, CosXmlClientException clientExcepion, CosXmlServiceException serviceException) {

                if (clientExcepion != null) {
                    // 客户端异常，例如网络异常等。
                    clientExcepion.printStackTrace();
                }
                if (serviceException != null) {
                    // 服务端异常。
                    Log.e("ErrorCode", serviceException.getErrorCode());
                    Log.e("RequestId", serviceException.getRequestId());
                    Log.e("HttpMessage", serviceException.getHttpMessage());
                    Log.e("Message", serviceException.getMessage());
                }
            }
        });
    }

    private void coscheckFileHash(String path) {
        String filename = FileHashUtil.getFileNameWithSuffix(path);
        String filedirname = FileHashUtil.getFileName(path) + ".txt";
        String cosdownloadPath = Environment.getExternalStorageDirectory()
                + File.separator + Environment.DIRECTORY_DCIM
                + File.separator + "cosdownload" + File.separator;
        try {
            File file = new File(getFilesDir()+ File.separator+ filedirname);
            FileInputStream in = new FileInputStream(file);
            byte[] checkbytes = new byte[in.available()];
            in.read(checkbytes);
            in.close();
            byte[] filebytes = FileHashUtil.hmacFiletoByte(cosdownloadPath + filename);

            if (Arrays.toString(checkbytes).equals(Arrays.toString(filebytes))) {
                Log.d(TAG, "cos success: "+ filedirname);
                    toastMessage("完整性校验成功");
            } else {
                Log.d(TAG, "cos fail: "+ filedirname);
                toastMessage("完整性校验失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    private void osscheckFileHash(String path) {
        String filename = FileHashUtil.getFileNameWithSuffix(path);
        String filedirname = FileHashUtil.getFileName(path) + ".txt";
        String cosdownloadPath = Environment.getExternalStorageDirectory()
                + File.separator + Environment.DIRECTORY_DCIM
                + File.separator + "cosdownload" + File.separator;

        try {
            File file2 = new File(getFilesDir()+ File.separator+ filedirname);
            FileInputStream in2 = new FileInputStream(file2);
            byte[] checkbytes2 = new byte[in2.available()];
            in2.read(checkbytes2);
            in2.close();
            byte[] filebytes2 = FileHashUtil.hmacFiletoByte(cosdownloadPath + "oss_" + filename);
            if (Arrays.toString(checkbytes2).equals(Arrays.toString(filebytes2))) {
                Log.d(TAG, "oss success: "+ filedirname);
                toastMessage("完整性校验成功");
            } else {
                Log.d(TAG, "oss fail: "+ filedirname);
                toastMessage("完整性校验失败");
            }
            file2.delete();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
