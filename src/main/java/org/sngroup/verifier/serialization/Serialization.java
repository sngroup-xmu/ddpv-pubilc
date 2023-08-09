package org.sngroup.verifier.serialization;

import org.sngroup.verifier.Subscriber;
import org.sngroup.verifier.TSBDD;
import org.sngroup.verifier.Context;

import java.io.InputStream;
import java.io.OutputStream;

public interface Serialization {
    void serialize(Context ctx, OutputStream os, TSBDD bdd);
    Context deserialize(InputStream is, TSBDD bdd);

    void serializeSubscribe(Subscriber subscriber, OutputStream os, TSBDD bdd);
    Subscriber deserializeSubscribe(InputStream is, TSBDD bdd);
}


