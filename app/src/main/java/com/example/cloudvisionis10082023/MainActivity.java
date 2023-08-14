package com.example.cloudvisionis10082023;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.FaceAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.Position;
import com.google.api.services.vision.v1.model.TextAnnotation;
import com.google.api.services.vision.v1.model.Vertex;
import com.google.common.collect.BiMap;

import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    Vision vision;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Vision.Builder visionBuilder = new Vision.Builder(new NetHttpTransport(),
                new AndroidJsonFactory(), null);
        visionBuilder.setVisionRequestInitializer(new VisionRequestInitializer("AIzaSyDMmRXHBYOjJyXZruXemR11tl7uiJ2T_Q8"));
        vision = visionBuilder.build();
    }

    public void OpenGallery(View view) {
        Intent i = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }
    private static int RESULT_LOAD_IMAGE = 1;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data)
        {
            ImageView imageView = (ImageView) findViewById(R.id.imagen);
            imageView.setImageURI(data.getData());
        }
    }

    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;
        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    public Image getImageToProcess(){
        ImageView imagen = (ImageView)findViewById(R.id.imagen);
        BitmapDrawable drawable = (BitmapDrawable) imagen.getDrawable();
        Bitmap bitmap = drawable.getBitmap();
        //bitmap = scaleBitmapDown(bitmap, 800);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        byte[] imageInByte = stream.toByteArray();
        Image inputImage = new Image();
        inputImage.encodeContent(imageInByte);
        return inputImage;
    }

    public BatchAnnotateImagesRequest setBatchRequest(String TipoSolic, Image inputImage){
        Feature desiredFeature = new Feature();
        desiredFeature.setType(TipoSolic);
        AnnotateImageRequest request = new AnnotateImageRequest();
        request.setImage(inputImage);
        request.setFeatures(Arrays.asList(desiredFeature));
        BatchAnnotateImagesRequest batchRequest = new BatchAnnotateImagesRequest();
        batchRequest.setRequests(Arrays.asList(request));
        return batchRequest;
    }

    public void ProcesarTexto(View View){
        print("HOLA");
        AsyncTask.execute(new Runnable() {

            @Override
            public void run() {
                BatchAnnotateImagesRequest batchRequest = setBatchRequest("TEXT_DETECTION", getImageToProcess());
                try {
                    Vision.Images.Annotate annotateRequest = vision.images().annotate(batchRequest);
                    annotateRequest.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    final TextAnnotation text = response.getResponses().get(0).getFullTextAnnotation();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView imageDetail = (TextView)findViewById(R.id.textView2);
                            imageDetail.setText(text.getText());
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void ProcesarImagen(View View){
        AsyncTask.execute(new Runnable() {

            @Override
            public void run() {
                BatchAnnotateImagesRequest batchRequest = setBatchRequest("LABEL_DETECTION", getImageToProcess());
                try {
                    Vision.Images.Annotate annotateRequest = vision.images().annotate(batchRequest);
                    annotateRequest.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse response = annotateRequest.execute();

                    final StringBuilder message = new StringBuilder("Se ha encontrado los siguientes Objetos:\n\n");
                    List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();

                    if (labels != null) {
                        for (EntityAnnotation label : labels)
                            message.append(String.format(Locale.US, "%.2f: %s\n", label.getScore() * 100, label.getDescription()));
                    } else {
                        message.append("No hay ningún Objeto");
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView imageDetail = (TextView) findViewById(R.id.textView2);
                            imageDetail.setText(message.toString());
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    public void ProcesarRostros(View View){
        print("HOLA");
        AsyncTask.execute(new Runnable() {

            @Override
            public void run() {
                BatchAnnotateImagesRequest batchRequest = setBatchRequest("FACE_DETECTION", getImageToProcess());
                try {
                    Vision.Images.Annotate annotateRequest = vision.images().annotate(batchRequest);
                    annotateRequest.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse response = annotateRequest.execute();


                    List<FaceAnnotation> faces = response.getResponses().get(0).getFaceAnnotations();
                    int numberOfFaces = faces.size();
                    String likelihoods = "";

                    FaceAnnotation cara;

                    ImageView imagen = (ImageView)findViewById(R.id.imagen);
                    BitmapDrawable drawable = (BitmapDrawable)imagen.getDrawable();
                    Bitmap bitmap = drawable.getBitmap().copy(Bitmap.Config.ARGB_8888, true);
                    Canvas canvas = new Canvas(bitmap);
                    Paint paint = new Paint();
                    paint.setColor(Color.RED);
                    paint.setTextSize(70);
                    paint.setStrokeWidth(20);


                    for (int i = 0; i<numberOfFaces ; i++){
                        cara = faces.get(i);

                        ArrayList<Vertex> vertices = new ArrayList<>();
                        int j = 0;
                        for (j = 0; j < cara.getBoundingPoly().getVertices().size(); j++)
                        {
                            if (j > 0)
                            {
                                Vertex verticei = cara.getBoundingPoly().getVertices().get(j);
                                Vertex verticef = cara.getBoundingPoly().getVertices().get(j - 1);


                                if (verticei.getX() == null) verticei.setX(0);
                                if (verticei.getY() == null) verticei.setY(0);

                                if (verticef.getX() == null) verticef.setX(0);
                                if (verticef.getY() == null) verticef.setY(0);

                                canvas.drawLine(
                                        verticei.getX(),
                                        verticei.getY(),
                                        verticef.getX(),
                                        verticef.getY(),
                                        paint);

                                imagen.setImageBitmap(bitmap);
                            }

                            //vertices.add(vertice);
                        }

                        Vertex verticei = cara.getBoundingPoly().getVertices().get(j - 1);
                        Vertex verticef = cara.getBoundingPoly().getVertices().get(0);

                        canvas.drawLine(
                                verticei.getX(),
                                verticei.getY(),
                                verticef.getX(),
                                verticef.getY(),
                                paint);

                        //canvas.drawText("HOMBRE",  verticef.getX(), verticei.getY() - 40, paint);

                        imagen.setImageBitmap(bitmap);
                        /*for (int j = 0; j < vertices.size(); j++){
                            if (j + 1 < vertices.size())
                            {
                                canvas.drawLine(vertices.get(j).getX(),
                                        vertices.get(j).getY(),
                                        vertices.get(j + 1).getX(),
                                        vertices.get(j + 1).getY(),
                                        paint);
                            }
                            else{
                                canvas.drawLine(vertices.get(j).getX(),
                                        vertices.get(j).getY(),
                                        vertices.get(0).getX(),
                                        vertices.get(0).getY(),
                                        paint);
                            }

                        }*/

                    }

                    imagen.setImageBitmap(bitmap);

                    final String message = "Esta imagen tiene " + numberOfFaces + " rostros " + likelihoods;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView imageDetail = (TextView)findViewById(R.id.textView2);
                            imageDetail.setText(message.toString());
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    public void print(String text){
        Log.i("_CONSOLA", text);
    }
}