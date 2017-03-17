package altea.mapa2;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

/**
 * Created by milandjoric on 3/12/17.
 */

public class ParkingMesto {

    String naziv;
    LatLng pozicija;
    LatLngBounds oblast;

    public ParkingMesto(String input) {
        String[] delovi = input.split(",");
        naziv = delovi[0];
        if (delovi.length == 7) {
            LatLng goreLevo = new LatLng(Double.parseDouble(delovi[1]), Double.parseDouble(delovi[2]));
            LatLng doleDesno = new LatLng(Double.parseDouble(delovi[3]), Double.parseDouble(delovi[4]));
            oblast = new LatLngBounds(goreLevo, doleDesno);
            pozicija = new LatLng(Double.parseDouble(delovi[5]), Double.parseDouble(delovi[6]));
        } else if (delovi.length == 4) {
            oblast = null;
            pozicija = new LatLng(Double.parseDouble(delovi[1]), Double.parseDouble(delovi[2]));
        } else {
            oblast = null;
            pozicija = new LatLng(0, 0);
        }
    }

    public ParkingMesto(String[] inputList){
        naziv = inputList[0];
        if (inputList.length == 7) {
            LatLng goreDesno = new LatLng(Double.parseDouble(inputList[1]), Double.parseDouble(inputList[2]));
            LatLng doleLevo = new LatLng(Double.parseDouble(inputList[3]), Double.parseDouble(inputList[4]));
            oblast = new LatLngBounds(doleLevo, goreDesno);
            pozicija = new LatLng(Double.parseDouble(inputList[5]), Double.parseDouble(inputList[6]));
        } else if (inputList.length == 4) {
            oblast = null;
            pozicija = new LatLng(Double.parseDouble(inputList[1]), Double.parseDouble(inputList[2]));
        } else {
            oblast = null;
            pozicija = new LatLng(0, 0);
        }
    }

}
