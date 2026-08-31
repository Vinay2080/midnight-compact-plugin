package dev.verloren.midnight.stdlib;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.psi.CompactFile;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class CompactStdlibServiceTest extends BasePlatformTestCase {

  public void testStdlibFilesAreLoadedAndCached() {
    CompactStdlibService service = CompactStdlibService.getInstance(getProject());
    List<CompactFile> files = service.getStandardLibraryFiles();
    assertNotNull("Stdlib files should not be null", files);
    assertFalse("Stdlib files should not be empty", files.isEmpty());
    assertEquals(2, files.size());

    // Calling again returns identical cached list instance
    assertSame("Second call should return cached instance", files, service.getStandardLibraryFiles());
  }

  public void testStdlibFilesModificationStampIsDeterministic() {
    CompactStdlibService service = CompactStdlibService.getInstance(getProject());
    List<CompactFile> files = service.getStandardLibraryFiles();
    for (CompactFile file : files) {
      assertNotNull(file);
      assertEquals("Stdlib file should have deterministic timestamp (0L)", 0L, file.getModificationStamp());
    }
  }

  public void testConcurrentStdlibAccess() throws InterruptedException {
    CompactStdlibService service = CompactStdlibService.getInstance(getProject());
    int threads = 4;
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threads);
    AtomicReference<Throwable> error = new AtomicReference<>();

    for (int i = 0; i < threads; i++) {
      executor.submit(() -> {
        try {
          startLatch.await();
          for (int j = 0; j < 50; j++) {
            List<CompactFile> files = service.getStandardLibraryFiles();
            assertNotNull(files);
            assertEquals(2, files.size());
          }
        } catch (Throwable t) {
          error.compareAndSet(null, t);
        } finally {
          doneLatch.countDown();
        }
      });
    }

    startLatch.countDown();
    boolean completed = doneLatch.await(5, TimeUnit.SECONDS);
    executor.shutdown();
    assertTrue("Concurrent tasks should complete within timeout", completed);
    assertNull("No errors should occur during concurrent stdlib access", error.get());
  }
}
