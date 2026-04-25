package listeners;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * RetryAnnotationTransformer — automatically wires RetryAnalyzer into every @Test method globally.
 *
 * Why: Without this transformer, you'd have to write @Test(retryAnalyzer = RetryAnalyzer.class)
 * on every single test method. That's noisy and easy to forget. This transformer runs once at
 * suite startup and injects the retryAnalyzer into every test annotation automatically.
 *
 * How it works:
 * - TestNG calls transform() for each @Test method before the suite starts.
 * - We set the retryAnalyzer class on the annotation — same effect as @Test(retryAnalyzer=...).
 * - This transformer must be registered in testng.xml as a <listener> to take effect.
 */
public class RetryAnnotationTransformer implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation,
                          Class testClass,
                          Constructor testConstructor,
                          Method testMethod) {
        // Inject RetryAnalyzer into every @Test method automatically
        annotation.setRetryAnalyzer(RetryAnalyzer.class);
    }
}
