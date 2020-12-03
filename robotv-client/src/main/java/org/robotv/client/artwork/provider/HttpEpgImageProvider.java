package org.robotv.client.artwork.provider;

import org.robotv.client.artwork.ArtworkHolder;
import org.robotv.client.model.Event;

import java.io.IOException;

public class HttpEpgImageProvider extends SimpleArtworkProvider {

    private final String mTemplateUrl;

    public HttpEpgImageProvider(String templateUrl) {
        mTemplateUrl = templateUrl;
    }

    @Override
    public ArtworkHolder search(Event event) throws IOException {
        if(mTemplateUrl.isEmpty()) {
            return null;
        }

        String url;

        try {
            url = String.format(mTemplateUrl, event.getEventId());
        }
        catch(java.util.IllegalFormatException e) {
            return null;
        }

        if(!checkUrlExists(url)) {
            return null;
        }

        return new ArtworkHolder(url, url);
    }
}
