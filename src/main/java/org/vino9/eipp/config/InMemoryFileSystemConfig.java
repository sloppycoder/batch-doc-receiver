package org.vino9.eipp.config;

import com.google.common.jimfs.Jimfs;
import java.nio.file.FileSystem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InMemoryFileSystemConfig {

    @Bean("inMemoryFileSystem")
    public FileSystem inMemoryFileSystem(){
        var configBuilder = com.google.common.jimfs.Configuration.unix().toBuilder();
        configBuilder.setMaxSize(1024*1024*512L); // 512M
        return Jimfs.newFileSystem(configBuilder.build());
    }
}
