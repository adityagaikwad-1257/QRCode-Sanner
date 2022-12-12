package com.adi.messages;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.adi.messages.databinding.ActivityMainBinding;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    /*
        TAG for debugging purpose
     */
    private static final String TAG = "aditya";

    /*
        using view binding to access the elements in the xml
     */
    ActivityMainBinding binding;

    /*
        @InputImage object required for @BarcodeScanner as a dependency
     */
    InputImage inputImage;

    /*
        holds the URI to the Image file created in the private space
     */
    private Uri capturedImageUri;

    /*
        @ActivityResultLauncher to launch camera
        to capture picture using System Camera app

        the image captured is written on the file created in private space
        with the help of the uri passed
     */
    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(),
            result -> {
                /*
                    result is of boolean type

                    tells if the image was captured and successfully written on the file present at the
                    passed uri
                 */
                try {
                    Log.d(TAG, "result from image capture: " + result);

                    if (result){
                        /*
                            converting captured image to @InputImage
                         */
                        inputImage = InputImage.fromFilePath(this, capturedImageUri);

                        /*
                            scanning inputImage retrieved
                        */
                        scanBarcode();
                    } else {
                        Toast.makeText(this, "please select am image :)", Toast.LENGTH_SHORT).show();
                    }

                } catch (NullPointerException e) {
                    Log.d(TAG, "null bitmap while capturing image: " + e.getMessage());
                    Toast.makeText(this, "please click an image", Toast.LENGTH_SHORT).show();
                } catch (Exception e){
                    Log.d(TAG, "error while capturing image: " + e.getMessage());
                    Toast.makeText(this, "something went wrong", Toast.LENGTH_SHORT).show();
                }
            });
    /*
        @ActivityResultLauncher to get content
        asking user to select an Image from their device

        takes @String as the type of data to be selected
     */
    private final ActivityResultLauncher<String> getContentLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
            activityResult -> {
                try {

                    /*
                        activityResult is of @Uri type
                        the URI of the selected image by the user

                        this activityResult i.e. URI of hte image selected if then converted to
                        @InputImage
                     */
                    inputImage = InputImage.fromFilePath(this, activityResult);

                    /*
                        scanning the inputImage
                     */
                    scanBarcode();

                }catch (NullPointerException e){
                    Log.d(TAG, "image from gallery is null");
                    Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
                }
                catch (IOException e) {
                    Log.d(TAG, "error while getting image input from path ");
                    Toast.makeText(this, "something went wrong", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        /*
            setting the custom action bar as the ActionBar to this Activity

            this automatically sets te title and its appearance
         */
        setSupportActionBar(binding.mainToolBar);

        /*
            getting imageUri to the image file in the private space of the app
         */
        capturedImageUri = createImageFile();

        /*
            on the click of Camera Image Option

            launching camera with cameraLauncher to click an Image of the QR code
            specifying the URI on which the CAMERA app should override the image
         */
        binding.optCamera.setOnClickListener(v -> cameraLauncher.launch(capturedImageUri));

        /*
            on the click of Gallery Image Option

            launching activity to select an image from the user device
            passing "image/*", specifying the type of document to be selected and * to specify the
            extension of the image i.e (Any extension)
         */
        binding.optGallery.setOnClickListener(v -> getContentLauncher.launch("image/*"));
    }

    /*
        creating an Image File in the private space of the app
        and returning its URI
     */
    private Uri createImageFile() {
        File imageFile = new File(getApplicationContext().getFilesDir(), "qrcode.jpg");

        /*
            A provider with authority : "com.adi.messages.file_provider" would allow writing on this file
            A uri is generated accordingly
         */
        return FileProvider.getUriForFile(getApplicationContext(), "com.adi.messages.file_provider",
                imageFile);
    }

    /*
        uses @BarcodeScanner to scan inputImage selected by the user
     */
    private void scanBarcode(){
        /*
            checking if the image is not yet selected and throwing NullPointException accordingly
         */
        if (inputImage == null) throw new NullPointerException();

        /*
            creating @BarcodeScannerOptions specifying the format
            as QR_CODE
            this is then used for creating @BarcodeScanner
         */
        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build();

        /*
            @BarcodeScanner creation using the provider
         */
        BarcodeScanner scanner = BarcodeScanning.getClient(options);

        /*
            processing the inputImage with @BarcodeScanner

            Also, adding Success and Failure Listeners
         */
        scanner.process(inputImage)
                .addOnSuccessListener(result -> {
                    /*
                        result is @List<@Barcode>
                        contains the result of the scanned QR_CODE

                        the result size may be 0
                     */
                    if (result.size() == 0){
                        Toast.makeText(this, "No QR code found in the Image", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    /*
                        getting the Barcode out of the list

                        getting the text (Raw value) in the QR_CODE using getRawValues() function in the @barcode

                        displaying the text on the Dialog box

                        Also, there could be different ways to use this retrieved data
                     */
                    showAlertDialog(result.get(0).getRawValue());

                }).addOnFailureListener(error -> {
                    Log.d(TAG, "scanBarcode: " + error.getMessage());
                    Toast.makeText(this, "something went wrong", Toast.LENGTH_SHORT).show();
                });
    }

    /*
        showing Alert Dialog and displaying raw values from QR_CODE
     */
    private void showAlertDialog(String rawValue) {
        Log.d(TAG, "showAlertDialog: " + rawValue);
        new AlertDialog.Builder(this)
                .setTitle("QR code result:")
                .setMessage(rawValue)
                .setPositiveButton("ok", (dialog, which) -> dialog.dismiss())
                .show();
    }
}