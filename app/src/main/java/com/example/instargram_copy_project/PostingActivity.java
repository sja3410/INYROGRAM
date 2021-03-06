package com.example.instargram_copy_project;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PostingActivity extends AppCompatActivity {

    private static final String TAG = "MemberInfoSetting";

    Button gallery_btn;
    Button posting_btn;
    Button cancel_btn;
    ImageView img_preview;
    ImageView img_view;

    private Uri filePath;

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser(); //user의 정보를 사용할것임
    FirebaseStorage storage = FirebaseStorage.getInstance();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    DocumentReference docRef = db.collection("Profile").document(user.getUid());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_posting);

        gallery_btn = findViewById(R.id.gallery_btn);
        cancel_btn = findViewById(R.id.cancelBtn);
        posting_btn = findViewById(R.id.posting_btn);
        img_preview = findViewById(R.id.img_preview);
        img_view = findViewById(R.id.img_view);

        gallery_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //이미지를 선택
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "이미지를 선택하세요."), 0);
            }
        });

        cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PostingActivity.this , HomeActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(0, 0);
            }
        });

        posting_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fileName = uploadFile();
                if(fileName != null) {
                    posting(fileName);
                }
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //request코드가 0이고 OK를 선택했고 data에 뭔가가 들어 있다면
        if (requestCode == 0 && resultCode == RESULT_OK) {
            filePath = data.getData();
            Log.d(TAG, "uri:" + String.valueOf(filePath));
            try {
                //Uri 파일을 Bitmap으로 만들어서 ImageView에 집어 넣는다.
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                img_preview.setImageBitmap(bitmap);
                img_view.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String uploadFile() {
        //업로드할 파일이 있으면 수행
        if (filePath != null) {
            //업로드 진행 Dialog 보이기
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("업로드중...");
            progressDialog.show();

            //storage

            //Unique한 파일명을 만들자.
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMHH_mmss");
            Date now = new Date();
            String filename = formatter.format(now) + ".png";
            //storage 주소와 폴더 파일명을 지정해 준다.
            StorageReference storageRef = storage.getReferenceFromUrl("gs://inyrogram.appspot.com").child("post_img/" + filename);

            //올라가거라...
            storageRef.putFile(filePath)
                    //성공시
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                            progressDialog.dismiss(); //업로드 진행 Dialog 상자 닫기
                            startToast("파일 업로드 완료!"); // 2. HomeActivity에서 뜸
                        }
                    })
                    //실패시
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
//                            progressDialog.dismiss();
                            startToast("파일 업로드 실패!");
                        }
                    })
                    //진행중
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            @SuppressWarnings("VisibleForTests") //이걸 넣어 줘야 아랫줄에 에러가 사라진다. 넌 누구냐?
                                    double progress = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                            //dialog에 진행률을 퍼센트로 출력해 준다
                            progressDialog.setMessage("Uploaded " + ((int) progress) + "% ...");
                        }
                    });
            return "post_img/" + filename;
        } else {
            startToast("파일을 선택해주세요!");
            return null;
        }
    }



    public void posting(String fileName) {
        String userUid = user.getUid(); //유저 UID 가져오기
        String place = ((EditText) findViewById(R.id.place)).getText().toString();
        String content = ((EditText) findViewById(R.id.content)).getText().toString();  //문구
//        Integer likeCount = 0;  //좋아요 수

        final Map<String, String> post = new HashMap<>();
        post.put("userUid", userUid);
        post.put("fileName", fileName);
        post.put("place", place);
        post.put("content", content);
        post.put("time" , Timestamp.now().toString());

        DocumentReference postDoc = db.collection("Post").document(user.getUid()).collection("privatePost").document();
        final String myId = postDoc.getId();
        postDoc.set(post, SetOptions.merge());
        DocumentReference postDoc1 = db.collection("Post").document(user.getUid()).collection("totalPost").document(myId);
        postDoc1.set(post, SetOptions.merge());
        db.collection("Follower").document(user.getUid()).collection("friends")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String friendid = document.getId();
                            startToast(myId + "\n" + friendid);
                            DocumentReference postDoc2 = db.collection("Post").document(friendid).collection("totalPost").document(myId);
                            postDoc2.set(post, SetOptions.merge());

                        }
                    }
                });

    startToast(postDoc.toString() + "업로드 성공!"); // 1. LoadingAcvitivy에서 뜸

        //홈 화면으로 이동
        Intent intent = new Intent(PostingActivity.this, LoadingActivity.class);
        intent.addFlags (Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        finish();
    }

    private void startToast(String msg){
        Toast.makeText(this,msg,Toast.LENGTH_LONG).show();
    }

    public void getDB(final String id, final String myId) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final Map<String, String> post = new HashMap<>();
        post.put("set", "1");
        db.collection("Follower").document(id).collection("friends")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            final HashMap<String, Object> map = new HashMap<String, Object>();
                            String friendid = document.getId();
                            DocumentReference postDoc1 = db.collection("Post").document(friendid).collection("totalPost").document(myId);
                            postDoc1.set(post, SetOptions.merge());

                        }


                }


                });//db.collection END
    }

}


