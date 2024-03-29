package tech.glasgowneuro.attysscope2;

/**
 * Calculates various signal stats. Just now pp and RMS
 */
//Only want pp in this project
public class SignalAnalysis {

    private int maxdata;
    private int maxdata2;
    private float[] data;
    private float[] data2;
    private int nData = 0;
    private int nData2 = 0;

    SignalAnalysis(int _maxata) {
        maxdata = _maxata;
        maxdata2 = _maxata;
        data = new float[maxdata];
        data2 = new float [maxdata2];
    }
    public void addData(float _data) {
        if (nData < maxdata) {
            data[nData] = 100 * _data;
            nData++;
        }
    }
    public void addData2 (float _data) {
        if (nData2 < maxdata2){
            data2[nData2] = _data;
            nData2 ++;
        }
    }

    public void reset() {
        nData = 0;
    }
    public void reset2(){
        nData2 = 0;
    }

    public int getNdata() {
        return nData;
    }
    public int getNData2(){
        return nData2;
    }

    public boolean bufferFull() {
        return nData == maxdata;
    }
    public boolean bufferFull2(){
        return nData2 == maxdata2;
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
    public float getPeakToPeak2(){
        float min = 1E10F;
        float max = -1E10F;
        if (nData2 > 0){
            for (int i = 0; i < nData2; i ++){
                float f = data2[i];
                if (f > max) max = f;
                if (f < min) min =f;

            }
        } else{
            max = 0;
            min = 0;
        }
        return max - min;
    }
    public  float getData(){
        return data[nData - 1];
    }
    public float getData2(){
        return data2[nData2 - 1];
    }
    public float[] getLast3Data(){
        float last3Data[] = new float[3];
        if(nData > 2){ // three data in buffer
            for(int i = 0; i < 3; i++){
                last3Data[i] = data[nData -3 + i];
            }
        }
        else if(nData == 2){
            for(int i = 0; i < 3; i ++){
                if (i == 0){
                    last3Data[i] = data[maxdata - 1];
                }
                else {
                    last3Data[i] = data[i - 1];
                }
            }
        }
        else if(nData == 1){
            for (int i = 0; i < 3; i++){
                if (i == 2){
                    last3Data[i] = data[0];
                } else{
                    last3Data[i] = data[maxdata - 2 + i];
                }
            }
        }
        else if(nData == 0){
            for(int i = 0; i < 3; i++){
                last3Data[i] = data[maxdata - 3 + i];
            }
        }
        return last3Data;
    }
    public float[] getLast3Data2(){
        float last3Data2 [] = new float[3];
        if(nData2 > 2){ // three data in buffer
            for(int i = 0; i < 3; i++){
                last3Data2[i] = data2[nData2 -3 + i];
            }
        }
        else if(nData2 == 2){
            for(int i = 0; i < 3; i ++){
                if (i == 0){
                    last3Data2[i] = data2[maxdata2 - 1];
                }
                else {
                    last3Data2[i] = data2[i - 1];
                }
            }
        }
        else if(nData2 == 1){
            for (int i = 0; i < 3; i++){
                if (i == 2){
                    last3Data2[i] = data2[0];
                } else{
                    last3Data2[i] = data2[maxdata2 - 2 + i];
                }
            }
        }
        else if(nData2 == 0){
            for(int i = 0; i < 3; i++){
                last3Data2[i] = data2[maxdata2 - 3 + i];
            }
        }
        return last3Data2;
    }
}
