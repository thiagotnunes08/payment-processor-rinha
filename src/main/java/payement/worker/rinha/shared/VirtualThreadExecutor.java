package payement.worker.rinha.shared;


import jakarta.inject.Singleton;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
public class VirtualThreadExecutor {

    public final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
}
