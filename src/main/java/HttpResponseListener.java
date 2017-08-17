package http_client;

/**
 * Created by lei on 17-8-9.
 */
public interface HttpResponseListener {
    void cb(Throwable e, String resp);
}
