package com.KawakawaPlanning.filetransfer;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.KawakawaPlanning.utility.FileSelectDialogFragment;
import com.KawakawaPlanning.utility.OnFileSelectedListener;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.GetDataCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class MainActivity extends FragmentActivity implements OnFileSelectedListener {
	
	//TODO 初期定義
	Vibrator vib;//バイブレーター定義
	String tag = "KP";//Log用tag定義
    ListView listView;//受信ファイル一覧用
    List<String> list;
    ProgressDialog waitDialog;
    AlertDialog dialogsend;
    ArrayAdapter<String> adapter ;
    AlertDialog.Builder alertDialogBuilder;
    String name;//自己ID定数
    String[] ObjID;
    String filePath;
    FileOutputStream fos;
    SharedPreferences pref;
    SharedPreferences.Editor editor; 
    boolean none;
    TextView welcomeText;
	//初期定義終了
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//オンクリでするのは関連づけ、および定義のみにしぼる。
		//起動時動作はonStartか、onResumeに
		listView = (ListView) findViewById(R.id.listView1);
		pref = getSharedPreferences("loginpref", Activity.MODE_PRIVATE); 
        list = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(this, R.layout.listview, list);
        fos = null;
        pref = getSharedPreferences("loginpref", Activity.MODE_PRIVATE); 
        welcomeText = (TextView)findViewById(R.id.welcomeTextView);
        vib = (Vibrator)getSystemService(VIBRATOR_SERVICE);
	}

	@Override
	protected void onStart(){
		super.onStart();
        Intent i = getIntent();
        name = i.getStringExtra("name");
        welcomeText.setText("ようこそ！" + name + "さん");
		check();
	}
	
    public void onClick(View v){
    	vib.vibrate(50);
    	switch (v.getId()) {
		case R.id.logoutbutton:
			Logout();
			break;

		case R.id.checkbutton:
			check();
			break;
			
		case R.id.sendbutton:
			sendbutton();
			break;
    	
    	}
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        
		menu.add(0, 0, 0, "詳細");
        menu.add(0, 10, 1, "削除");
    }
  
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {//もし選択されたのが
            case 0 ://詳細なら
                ParseQuery<ParseObject> qu = ParseQuery.getQuery("TestObject");
                qu.getInBackground(ObjID[1], new GetCallback<ParseObject>() {
                    public void done(ParseObject object, ParseException e) {
            	        if (e == null) {
            	    	        alert("詳細情報","送信者:" + object.getString("Sender") + "\n" + "ファイルID:" + object.getObjectId() + "\n" + "ファイル名:"+object.getString("FileName") + "\n" + "送信日:"+object.getCreatedAt());
            	        }else{
            	        	if (e.getCode() == 100){
					      	        alert("接続エラー","サーバーに接続できません。インターネット状態を確認してください。エラーコード:100");
					      	   }else{
					      	        alert("エラー","エラーが発生しました。少し時間を空けてお試しください。それでも直らない際はサポートに連絡してください。エラーコード:" + e.getCode());
					      	   }
            	        }
            	    }
                    });
                return true;
            case 10 ://削除なら
                //選択されたアイテムのobidで削除＝＞check();
                ParseQuery<ParseObject> quer = ParseQuery.getQuery("TestObject");
                quer.getInBackground(ObjID[1], new GetCallback<ParseObject>() {
                    public void done(final ParseObject object, ParseException e) {
            	        if (e == null) {
        	    	        alertDialogBuilder.setTitle("確認");
        	    	        alertDialogBuilder.setMessage("このファイルはダウンロードされていません。削除してもいいですか？");
        	    	        alertDialogBuilder.setPositiveButton("OK",
        	    	                new DialogInterface.OnClickListener() {
        	    	                    @Override
        	    	                    public void onClick(DialogInterface dialog, int which) {
        	    	                    	object.deleteInBackground();
        	    	                    	check();
        	    	                    }
        	    	                });
        	    	        alertDialogBuilder.setNegativeButton("キャンセル",null);
        	    	        alertDialogBuilder.show();

            	        }else{
            	        	if (e.getCode() == 100){
					      	        alert("接続エラー","サーバーに接続できません。インターネット状態を確認してください。エラーコード:100");
					      	   }else{
					      	        alert("エラー","エラーが発生しました。少し時間を空けてお試しください。それでも直らない際はサポートに連絡してください。エラーコード:" + e.getCode());
					      	   }
            	        }
                    }
                });
                return true;
  }
        return super.onContextItemSelected(item);
}
    
    @Override
 	protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
     	if(requestCode == 1 && resultCode == RESULT_OK) {
     		final String path = getPath(MainActivity.this, data.getData());
     		final String Fi[] = path.split("/");
     		final File file = new File(path);
     		long size = file.length();
     		long out = 10485760;
     		if (size <= out){
     			final EditText editView = new EditText(MainActivity.this);
     			new AlertDialog.Builder(MainActivity.this)
     	        .setIcon(android.R.drawable.ic_dialog_info)
     	        .setTitle("送信先設定")
     	        //setViewにてビューを設定します。
     	        .setMessage("送信する相手のIDを入力して下さい。")
     	        .setView(editView)
     	        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
     	            public void onClick(DialogInterface dialog, int whichButton) {
     	            	
     	            	if (editView.getEditableText().toString().equals("")){
     	    				none = true;
     	    				Toast.makeText(MainActivity.this,"送信する相手のIDを入力してください", Toast.LENGTH_LONG);
     	    			}else{
     	    				Wait("送信処理");
     	                    (new Thread(new Runnable() {
     	                    	@Override
     	                    		public void run() {
     			try {
     				byte[] dat = readFileToByte(path);
     				ParseObject jobApplication = new ParseObject("TestObject");
     		  		ParseFile file = new ParseFile("data.bin", dat);
     	    		ParseACL acl= new ParseACL();
     	    		acl.setPublicReadAccess(true);
     	    		acl.setPublicWriteAccess(true);
     	    		jobApplication.put("Picture", file);
     	    		jobApplication.put("FileName",Fi[Fi.length-1]);
     	    		jobApplication.put("Sender", name);
     	    		jobApplication.put("Receiver", editView.getEditableText().toString());
     	    		jobApplication.setACL(acl);
     	    		jobApplication.saveInBackground(new SaveCallback() {
     					
     					@Override
     					public void done(ParseException e) {
     						// TODO 自動生成されたメソッド・スタブ
     						if(e == null){
     							waitDialog.dismiss();
     							Toast.makeText(MainActivity.this, "送信完了", Toast.LENGTH_SHORT).show();
     							}else{
     							if (e.getCode() == 100){
     					      	        alert("接続エラー","サーバーに接続できません。インターネット状態を確認してください。エラーコード:100");
     					      	   }else{
     					      	        alert("エラー","エラーが発生しました。少し時間を空けてお試しください。それでも直らない際はサポートに連絡してください。エラーコード:" + e.getCode());
     					      	   }
     						}
     					}
     				});
     	    	} catch (Exception e) {
     	    	}
     	                }
     	              })).start();
     	    		}
     	    }
     	})
     	.setNegativeButton("キャンセル", null).show();
     		}else{
     			Toast.makeText(MainActivity.this,"10MB以下のファイルしか送信できません",Toast.LENGTH_LONG).show();
     		}
     	}
         
 	if(requestCode == 0 && resultCode == RESULT_OK) {
 	    final String file;//string file定義
 	    Uri selectedImageUri = data.getData();//インテントの中からUriを抽出
 	    file = ImageFilePath.getPath(getApplicationContext(), selectedImageUri);//選択された画像のパスからfile作成
 	    
 		long size = file.length();//ファイル容量取得
 		long out = 10485760;//10MB定義
 		if (size <= out){//10MBを超えていなければ
 		final String Fi[] = file.split("/");
 		final EditText editView = new EditText(MainActivity.this);
 	    new AlertDialog.Builder(MainActivity.this)
         .setIcon(android.R.drawable.ic_dialog_info)
         .setTitle("送信先設定")
         .setMessage("送信する相手のIDを入力して下さい。")
         .setView(editView)
         .setPositiveButton("OK", new DialogInterface.OnClickListener() {
             public void onClick(DialogInterface dialog, int whichButton) {
 	            	if (editView.getEditableText().toString().equals("")){
 	    				none = true;
 	    				Toast.makeText(MainActivity.this,"送信する相手のIDを入力してください", Toast.LENGTH_LONG).show();
 	    			}else{
 	    				Wait("送信処理");//送信処理を表示
             	
                     (new Thread(new Runnable() {//裏で動かす
                     	@Override
                     		public void run() {
 		try {
 			byte[] dat = readFileToByte(file);
 			ParseObject jobApplication = new ParseObject("TestObject");
 	  		ParseFile file = new ParseFile("data.bin", dat);
     		ParseACL acl= new ParseACL();
     		acl.setPublicReadAccess(true);
     		acl.setPublicWriteAccess(true);
     		jobApplication.put("Picture", file);
     		jobApplication.put("FileName",Fi[Fi.length-1]);
     		jobApplication.put("Sender", name);
     		jobApplication.put("Receiver", editView.getEditableText().toString());
     		jobApplication.setACL(acl);
     		jobApplication.saveInBackground(new SaveCallback() {
 				@Override
 				public void done(ParseException e) {
 					waitDialog.dismiss();//ダイアログ終了
 					if(e == null){//エラーが無ければ
 					Toast.makeText(MainActivity.this, "送信完了", Toast.LENGTH_SHORT).show();
 					}else{
 						if (e.getCode() == 100){
 				      	        alert("接続エラー","サーバーに接続できません。インターネット状態を確認してください。エラーコード:100");
 				      	   }else{
 				      	        alert("エラー","エラーが発生しました。少し時間を空けてお試しください。それでも直らない際はサポートに連絡してください。エラーコード:" + e.getCode());
 				      	   }
 					}
 				}
 			});
     	} catch (Exception e) {
     	}
             	}
             })).start();
 	    			}
     }
 })
 .setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
     public void onClick(DialogInterface dialog, int whichButton) {
     }
 })
 .show();
 	}
 	}
    }
    
    public void check(){
    	
    	Wait("受信ファイル読み込み");
    	list.removeAll(list);//リストのアダプタからすべて削除する
    	listView.setAdapter(adapter);//アダプタの内容を適用する
    	adapter.notifyDataSetChanged();//アダプタの更新？
       	ParseQuery<ParseObject> query = ParseQuery.getQuery("TestObject");//ParseObject型のParseQueryを取得する。
    	query.whereEqualTo("Receiver", name);//そのクエリの中でReceiverがname変数のものを抜き出す。
    	query.findInBackground(new FindCallback<ParseObject>() {
    	    public void done(List<ParseObject> parselist, com.parse.ParseException e) {//その、name変数のものが見つかったとき
    	        if (e == null) {//エラーが無ければ
    	        	for(int i=0;i != parselist.size() ;i++){//iがヒットした数になるまで繰り返す。
    	            	ParseObject PO = parselist.get(i);//クエリ野中でi番目のデータをParseObject型で取得する
    	            	String obId = PO.getObjectId();//ParseObjectからObjectIDを取得する
                        ParseQuery<ParseObject> que = ParseQuery.getQuery("TestObject");//その、ObjectIDで参照できるデータの内容をParseObject型のParseQueryで取得
                        que.getInBackground(obId, new GetCallback<ParseObject>() {
                            public void done(ParseObject object, ParseException e) {//取得に成功したとき
                            	String str = "送信者:" + object.getString("Sender") +"/ファイル名:" 
                            + object.getString("FileName")+ "/ファイルID:"+(String)object.getObjectId();//strにリストに表示する内容を入れる。
                            	list.add(str);//リストにstrを追加
                            	listView.setAdapter(adapter);//アダプターを適用する
                            }
                            });
    	        		}
    	        }
	            waitDialog.dismiss();//読み込み中ダイアログ停止
	        	Toast.makeText(MainActivity.this, "読み込み完了", Toast.LENGTH_SHORT).show();
    	    }
    	});
    	
        listView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
            	String text = (String)parent.getItemAtPosition(position);
                String objc[] = text.split("/");
                ObjID = objc[2].split(":");
				return false;
			}
		});
        
    	listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?>parent, View view, int position, long id){
                //TODO 主に受信時
            	Wait("受信処理");//受信ダイアログ表示
            	String text = (String)parent.getItemAtPosition(position);//選択したリストの文字列代入
                String a[] = text.split("/"); ObjID = a[2].split(":");//ObjectID取得
                ParseQuery<ParseObject> que = ParseQuery.getQuery("TestObject");//ParseObject型のParseQueryを取得する。
                que.getInBackground(ObjID[1], new GetCallback<ParseObject>() {
                    public void done(final ParseObject object, ParseException e) {//成功時
                    	filePath = Environment.getExternalStorageDirectory() +"/Download/"+ object.getString("FileName");//ダウンロードしたファイル保存する場所
                        ParseFile applicantResume = (ParseFile)object.get("Picture");//列名がPictureのファイルをParseFileに入れる
                        applicantResume.getDataInBackground(new GetDataCallback() {//受信処理
                            public void done(byte[] data, ParseException e) {//成功時
                                if (e == null) {//エラーが無いなら
                                	try{
                                	fos = new FileOutputStream(filePath);//FileOutputStreamに保存先指定
                                	fos.write(data);//実際に書き込み
                                	fos.close();//終了
                                	waitDialog.dismiss();//ダイアログ終了
                                	object.deleteInBackground();//ダウンロードしたファイル削除
                                	check();//受信
                                	Toast.makeText(MainActivity.this, "受信完了", Toast.LENGTH_SHORT).show();
                                	}catch(Exception er){
                                		er.printStackTrace();
                                	}
                                }
                            }
                        });
                        }
                    });
            }
        });
        
    }
    public void sendbutton() {
	       AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
	        alertDialogBuilder.setTitle("種類選択");
	        alertDialogBuilder.setMessage("送信するファイルの種類を選択してください。");
	        alertDialogBuilder.setPositiveButton("写真",//写真が選択されたら
	                new DialogInterface.OnClickListener() {
	                    @Override
	                    public void onClick(DialogInterface dialog, int which) {
	                    	if (Build.VERSION.SDK_INT < 19) {//kitkat以上なら
	                    			    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
	                    			    intent.setType("image/jpeg");
	                    			    startActivityForResult(intent , 1);
	                    			  } else {//jellybean以下なら
	                    			    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
	                    			    intent.addCategory(Intent.CATEGORY_OPENABLE);
	                    			    intent.setType("image/jpeg");
	                    			    startActivityForResult(intent, 0);
	                    			  }
	                    }
	                });
	        alertDialogBuilder.setNegativeButton("それ以外",//それ以外が選択されたら
	                new DialogInterface.OnClickListener() {
	                    @Override
	                    public void onClick(DialogInterface dialog, int which) {
	                	FileSelectDialogFragment dialogFragment = new FileSelectDialogFragment();
	               		Bundle bundle = new Bundle();
	               		bundle.putString(FileSelectDialogFragment.ROOT_DIRECTORY, "/");
	               		bundle.putString(FileSelectDialogFragment.INITIAL_DIRECTORY, Environment.getExternalStorageDirectory().getPath());
	               		bundle.putString(FileSelectDialogFragment.PREVIOUS, "..");
	               		bundle.putString(FileSelectDialogFragment.CANCEL, "キャンセル");
	               		bundle.putSerializable(FileSelectDialogFragment.LISTENER, MainActivity.this);
	               		dialogFragment.setArguments(bundle);
	               		dialogFragment.setCancelable(false);
	               		dialogFragment.show(MainActivity.this.getSupportFragmentManager(), "dialog");
	                    }
	                });
	        alertDialogBuilder.show();
	}
    
    
    @Override
    public void onFileSelected(final String path) {
		final File file = new File(path);//選択されたPathからFileを取得
		long size = file.length();//ファイルの容量代入
		long out = 10485760;//限度容量代入
		if (size <= out){//10MBを超えていなければ
		final EditText editView = new EditText(MainActivity.this);
	    	none = false;
	    	if(dialogsend == null) {
		dialogsend = new AlertDialog.Builder(MainActivity.this)
        .setIcon(android.R.drawable.ic_dialog_info)
        .setTitle("送信先設定")
        .setMessage("送信する相手のIDを入力して下さい。")
        .setView(editView)
        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	if (editView.getEditableText().toString().equals("")){
				none = true;
				Toast.makeText(MainActivity.this,"送信する相手のIDを入力してください",Toast.LENGTH_LONG).show();
			}else{
				Wait("送信処理");
            	none = false;
                    (new Thread(new Runnable() {
                    	@Override
                    		public void run() {
		try {
			
				none = false;
			ParseObject jobApplication = new ParseObject("TestObject");
			
	  		ParseFile file = new ParseFile("data.bin", readFileToByte(path));
    		
	  		String xx = new String(readFileToByte(path), "UTF-8"); //   
	  		final String a[] = path.split("/");
    		ParseACL acl= new ParseACL();
    		acl.setPublicReadAccess(true);
    		acl.setPublicWriteAccess(true);
    		jobApplication.put("Picture", file);
    		jobApplication.put("FileName",a[a.length-1]);
    		jobApplication.put("Sender", name);
    		jobApplication.put("Receiver", editView.getEditableText().toString());
    		jobApplication.setACL(acl);
    		jobApplication.saveInBackground(new SaveCallback() {
				
				@Override
				public void done(ParseException e) {
					// TODO 自動生成されたメソッド・スタブ
					if(e == null){
					Toast.makeText(MainActivity.this, "送信完了", Toast.LENGTH_SHORT).show();
					waitDialog.dismiss();
					}else{
						if (e.getCode() == 100){
				      	      alert("接続エラー","サーバーに接続できません。インターネット状態を確認してください。エラーコード:100");
				      	   }else{
				      	        alert("エラー","エラーが発生しました。少し時間を空けてお試しください。それでも直らない際はサポートに連絡してください。エラーコード:" + e.getCode());
				      	   }
					}
				}
			});
    	} catch (Exception e) {
    	}
            	}
            })).start();
			}
            }		
        	})
		.setNegativeButton("キャンセル",null).create();
	    }
	    	dialogsend.show();
	    
		}else{
			Toast.makeText(MainActivity.this, "10MB以下のファイルしか送信できません",Toast.LENGTH_LONG).show();
		}
    }
    	
    @Override
    public void onFileSelectCanceled() {
    }
    
    private void Wait(String what){
        waitDialog = new ProgressDialog(MainActivity.this);
        waitDialog.setMessage(what + "中...");
        waitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        waitDialog.setCanceledOnTouchOutside(false);
        waitDialog.show();
    }
    private void alert(String til,String msg){
		alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
 	    alertDialogBuilder.setTitle(til);
 	    alertDialogBuilder.setMessage(msg);
 	    alertDialogBuilder.setPositiveButton("OK",null);
 	    alertDialogBuilder.show();
    }
    private void Logout(){
    	ParseUser.logOut();
    	editor = pref.edit();  
	      editor.putBoolean("flag",false); 
	      // データの保存  
	      editor.commit(); 
    	Intent intent=new Intent();
        intent.setClassName("com.KawakawaPlanning.filetransfer","com.KawakawaPlanning.filetransfer.StartActivity");
        startActivity(intent);
    	finish();
    }
    private byte[] readFileToByte(String filePath) throws Exception {
        byte[] b = new byte[1];
        FileInputStream fis = new FileInputStream(filePath);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (fis.read(b) > 0) {
            baos.write(b);
        }
        baos.close();
        fis.close();
        b = baos.toByteArray();
        return b;
    }
    private static String getPath(Context context, Uri uri) {
        ContentResolver contentResolver = context.getContentResolver();
        String[] columns = { MediaStore.Images.Media.DATA };
        Cursor cursor = contentResolver.query(uri, columns, null, null, null);
        cursor.moveToFirst();
        String path = cursor.getString(0);
        cursor.close();
        return path;
    }
}
