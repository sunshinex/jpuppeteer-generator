package jpuppeteer;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

public class GenTest {

    @Rule
    public MojoRule rule = new MojoRule();

    @Rule
    public TestResources resources = new TestResources();

    @Test
    public void testGen() throws Exception {
        File projectCopy = this.resources.getBasedir("gen");
        File pom = new File( projectCopy, "pom.xml");
        Assert.assertNotNull( pom );
        Assert.assertTrue( pom.exists());
        Mojo mojo = rule.lookupMojo("gen", pom);
        Assert.assertNotNull( mojo );
        mojo.execute();
    }
}
