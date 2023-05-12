package com.vismut_fo.forestnavigator;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;

public class Network {
    static final float[] NO_MEAN_RGB = new float[] {0.5f, 0.5f, 0.5f};
    static final float[] NO_STD_RGB = new float[] {0.5f, 0.5f, 0.5f};

    final Module module;
    private final MainActivity owner;

    Network(MainActivity owner) {
        this.owner = owner;
        try {
            module = LiteModuleLoader.load(getModuleFilePath((Context)this.owner, "model2.ptl"));
        } catch (IOException e) {
            Log.d("<Pytorch>", this.owner.getFilesDir().getAbsolutePath(), e);
            throw new RuntimeException(this.owner.getFilesDir().getAbsolutePath());
        }
    }

    private static String getModuleFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = Files.newOutputStream(file.toPath())) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }



    synchronized public void run(Bitmap source) {
        Log.d("<Layers>", "NetworkRunStart");

        new Thread(new Runnable() {
            final Bitmap bitmapTemp = source;
            synchronized public void run() {

                Log.d("<Layers>", "InRun");

                Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmapTemp, 512, 512, false);

                Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap,
                        NO_MEAN_RGB, NO_STD_RGB);

                float[] inputData = inputTensor.getDataAsFloatArray();
                Log.d("<Layers>", "input length: " + Integer.toString(inputData.length));

                Log.d("<Layers>", "onNetworkStart");
                Map<String, IValue> outTensors = module.forward(IValue.from(inputTensor)).toDictStringKey();
                final Tensor outputTensor = Objects.requireNonNull(outTensors.get("out")).toTensor();

                Log.d("<Layers>", "onNetworkEnd");
                float[] scores = outputTensor.getDataAsFloatArray(); // ?
                Log.d("<Layers>", "turnedTensorToArray");

                owner.onNetworkEnd(scores);

                Log.d("<Layers>", "onModuleEnd");

            }
        }).start();
    }
}
