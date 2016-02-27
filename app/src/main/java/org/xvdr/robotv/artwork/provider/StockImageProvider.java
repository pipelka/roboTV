package org.xvdr.robotv.artwork.provider;

import org.xvdr.robotv.artwork.ArtworkHolder;
import org.xvdr.robotv.artwork.Event;

public class StockImageProvider extends SimpleArtworkProvider {

    private String getStockBackground(int contentId) {
        switch(contentId) {
                // Football / Soccer
            case 0x43:
                return "https://raw.githubusercontent.com/pipelka/roboTV/master/media/stock/sport-football.jpg";

                // Tennis
            case 0x44:
                return "https://raw.githubusercontent.com/pipelka/roboTV/master/media/stock/sport-tennis.jpg";

                // Athletics
            case 0x46:
                return "https://raw.githubusercontent.com/pipelka/roboTV/master/media/stock/sport-athletics.jpg";

                // Wintersports
            case 0x49:
                return "https://raw.githubusercontent.com/pipelka/roboTV/master/media/stock/sport-winter.jpg";

                // Equestrian
            case 0x4A:
                return "https://raw.githubusercontent.com/pipelka/roboTV/master/media/stock/sport-equestrian.jpg";
        }

        return "";
    }

    @Override
    public ArtworkHolder search(Event event) {
        return new ArtworkHolder(
                   "",
                   getStockBackground(event.getContentId())
               );
    }
}
