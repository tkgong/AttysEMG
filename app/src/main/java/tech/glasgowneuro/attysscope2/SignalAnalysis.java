package tech.glasgowneuro.attysscope2;

/**
 * Calculates various signal stats. Just now pp and RMS
 */
//Only want pp in this project
public class SignalAnalysis {

    private int maxdata;
    private float[] data;
    private int nData = 0;

    SignalAnalysis(int _maxata) {
        maxdata = _maxata;
        data = new float[maxdata];
    }

    public void addData(float _data) {
        if (nData < maxdata) {
            data[nData] = _data;
            nData++;
        }
    }

    public void reset() {
        nData = 0;
    }

    public int getNdata() {
        return nData;
    }

    public boolean bufferFull() {
        return nData == maxdata;
    }

    public float getPeakToPeak() {
        float min = 1E10F;
        float max = -1E10F;
        if (nData > 0) {
            for (int i = 0; i < nData; i++) {
                float f = data[i];
                if (f>max) max = f;
                if (f<min) min = f;
            }
        } else {
            max = 0;
            min = 0;
        }
        return max - min;
    }
}
