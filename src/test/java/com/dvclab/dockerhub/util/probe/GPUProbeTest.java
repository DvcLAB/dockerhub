package com.dvclab.dockerhub.util.probe;


import com.dvclab.dockerhub.model.Host;
import com.jcraft.jsch.JSchException;
import one.rewind.db.exception.DBInitException;
import one.rewind.monitor.CPUInfo;
import one.rewind.monitor.GPUInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;

public class GPUProbeTest {
    @Test
    public void testGPUProbe() throws JSchException, IOException, InterruptedException, SQLException, DBInitException {
        Host host = Host.getById(Host.class, "22636f682ce79b39d1bf61502279085c");
        host.connectSshHost();

        CPUInfo ci = new CPUInfo().probe(host.sshHost);
        System.out.println(ci.usage);
        GPUInfo gi = new GPUInfo().probe(host.sshHost);
        System.out.println(gi.cuda_version);

    }
}
