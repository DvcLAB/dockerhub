package com.dvclab.dockerhub.route;

import com.dvclab.dockerhub.auth.KeycloakAdapter;
import com.dvclab.dockerhub.cache.ContainerCache;
import com.dvclab.dockerhub.model.Container;
import com.dvclab.dockerhub.model.DataSet;
import com.dvclab.dockerhub.model.Image;
import com.dvclab.dockerhub.model.Project;
import com.dvclab.dockerhub.serialization.Msg;
import one.rewind.util.FileUtil;
import spark.Route;

public class ContainerRoute {


	static String tpl;

	static int jupyter_port = 8898;

	static String frp_server_addr = "j.dvclab.com";
	static int frp_server_port = 53000;

	static {
		tpl = FileUtil.readFileByLines("tpl/jupyterlab-inst.yaml");
	}

	public static Route createContainerDockerComposeConfig = (q, a) -> {

		String uid = q.session().attribute("uid");

		String image_id = q.queryParams("image_id");
		String project_id = q.queryParams("project_id");
		String project_branch = q.queryParams("project_id");
		String dataset_id = q.queryParams("dataset_id");

		float cpus = Float.parseFloat(q.queryParamOrDefault("cpus", "2"));
		float mem = Float.parseFloat(q.queryParamOrDefault("mem", "4"));
		boolean gpu = Boolean.parseBoolean(q.queryParamOrDefault("gpu", "false"));

		try {

			Image image = Image.getById(Image.class, image_id);
			Project project = Project.getById(Project.class, project_id);
			DataSet dataset = DataSet.getById(DataSet.class, dataset_id);

			Container container = ContainerCache.createContainer(uid, cpus, mem, jupyter_port, frp_server_addr);

			String docker_compose_config = tpl.replaceAll("\\$\\{image_name\\}", image.name)
					.replaceAll("\\$\\{container_id\\}", container.id)
					.replaceAll("\\$\\{jupyter_port\\}", String.valueOf(jupyter_port))
					.replaceAll("\\$\\{project_git_url\\}", project.url)
					.replaceAll("\\$\\{project_branch\\}", project_branch)
					.replaceAll("\\$\\{project_name\\}", project.name)
					.replaceAll("\\$\\{uid\\}", uid)
					.replaceAll("\\$\\{keycloak_server_addr\\}", KeycloakAdapter.getInstance().host)
					.replaceAll("\\$\\{keycloak_realm\\}", KeycloakAdapter.getInstance().realm)
					.replaceAll("\\$\\{client_id\\}", KeycloakAdapter.getInstance().frontend_client_id)
					.replaceAll("\\$\\{client_secret\\}", KeycloakAdapter.getInstance().frontend_client_secret)
					.replaceAll("\\$\\{container_login_url\\}", container.jupyter_url + "/login")
					.replaceAll("\\$\\{kafka_server\\}", ContainerCache.kafka_server_addr)
					.replaceAll("\\$\\{kafka_topic\\}", ContainerCache.container_event_topic_name)
					.replaceAll("\\$\\{frp_server_addr\\}", frp_server_addr)
					.replaceAll("\\$\\{frp_server_port\\}", String.valueOf(frp_server_port))
					.replaceAll("\\$\\{frp_remote_port\\}", String.valueOf(container.tunnel_port))
					.replaceAll("\\$\\{cpus\\}", String.valueOf(container.cpus))
					.replaceAll("\\$\\{mem\\}", String.valueOf(container.mem));

			if(gpu) {
				docker_compose_config = docker_compose_config
						.replaceAll("\\$\\{runtime\\}", "runtime: nvidia");
			}

			return Msg.success(docker_compose_config);
		}
		catch (Exception e) {

			Routes.logger.error("Unable create container, uid[{}], ", uid, e);
			return Msg.failure(e);
		}
	};
}
