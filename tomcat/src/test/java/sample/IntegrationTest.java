package sample;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.config.runtime.DefaultConfigurationService;
import io.javaoperatorsdk.operator.sample.Tomcat;
import io.javaoperatorsdk.operator.sample.TomcatController;
import io.javaoperatorsdk.operator.sample.WebappController;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

public class IntegrationTest {
    @Test
    public void test() throws InterruptedException {
        Config config = new ConfigBuilder().withNamespace(null).build();
        KubernetesClient client = new DefaultKubernetesClient(config);
        Operator operator = new Operator(client, DefaultConfigurationService.instance());

        TomcatController tomcatController = new TomcatController(client);
        operator.register(tomcatController);

        operator.register(new WebappController(client));

        Tomcat tomcat = loadYaml(Tomcat.class, "tomcat-sample1.yaml");

        MixedOperation<Tomcat, KubernetesResourceList<Tomcat>, Resource<Tomcat>> tomcatClient = client.customResources(Tomcat.class);

        Namespace tt_ns = new NamespaceBuilder().withMetadata(new ObjectMetaBuilder().withName("tomcat-test").build()).build();

        client.namespaces().delete(tt_ns);

        while (client.namespaces().withName("tomcat-test").get() != null) {
            Thread.sleep(1000);
        };

        client.namespaces().createOrReplace(tt_ns);

        tomcatClient.inNamespace("tomcat-test").create(tomcat);

        Tomcat updatedTomcat = tomcatClient.inNamespace("tomcat-test").withName("test-tomcat1").get();

        Thread.sleep(1000);

        assertNotNull(updatedTomcat.getStatus());
    }

    private <T> T loadYaml(Class<T> clazz, String yaml) {
        try (InputStream is = new FileInputStream("k8s/" + yaml)) {
            return Serialization.unmarshal(is, clazz);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot find yaml on classpath: " + yaml);
        }
    }
}
