package com.altimeter.bdureau.bearconsole.Flash;

/**
 * ============================================================================
 * Project: BearConsole - Model Rocketry Altimeter Management System
 * Class: UploadSTM32CallBack
 * Author: Boris du Reau (boris.dureau@neuf.fr)
 *
 * @description:
 *  Callback interface notified by the STM32 flashing code: upload start, progress (percentage),
 *  end and errors, so that the flashing screen can display the progress.
 * ============================================================================
 */

public interface UploadSTM32CallBack {

    /*
     * Callback methods
     */
    void onPreUpload();
    void onUploading(int value);
    void onInfo(String value);
    void onPostUpload(boolean success);
    void onCancel();
    void onError(UploadSTM32Errors err);

}
