package biz.aQute.gogo.commands.provider.http;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.apache.felix.service.command.annotations.GogoCommand;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;

import biz.aQute.gogo.commands.dtoformatter.DTOFormatter;

@Component(service = Object.class, immediate = true)
@GogoCommand(scope = "dto", function = {
	"httpruntimes", "httpruntime", "requestInfo"
})
public class HTTP {

	@Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
	volatile List<HttpServiceRuntime>	serviceRuntimes;

	@Reference(cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
	HttpServiceRuntime					defaultServiceRuntime;

	@Reference
	DTOFormatter						formatter;

	@Activate
	void activate() {
		// todo: formatter

	}

	@Descriptor("Show the RuntimeDTO of the HttpServiceRuntime")
	public List<RuntimeDTO> httpruntimes() throws InterruptedException {

		return serviceRuntimes.stream()
			.map(HttpServiceRuntime::getRuntimeDTO)
			.collect(Collectors.toList());
	}

	@Descriptor("Show the RuntimeDTO of the HttpServiceRuntime")
	public RuntimeDTO httpruntime(@Descriptor("Port")
	@Parameter(absentValue = "", names = "-p")
	String port) throws InterruptedException {

		return runtime(port).map(HttpServiceRuntime::getRuntimeDTO)
			.orElse(null);
	}

	@Descriptor("Show the RequestInfoDTO of the HttpServiceRuntime")
	public RequestInfoDTO requestInfo(@Descriptor("Port")
	@Parameter(absentValue = "", names = "-p")
	String port, @Descriptor("Path")
	@Parameter(absentValue = "", names = "-pa")
	String path) throws InterruptedException {

		return runtime(port).map(runtime -> runtime.calculateRequestInfoDTO(path))
			.orElse(null);
	}

	private Optional<HttpServiceRuntime> runtime(String port) {

		if (port.isEmpty()) {
			return Optional.ofNullable(defaultServiceRuntime);
		}
		return serviceRuntimes.stream()
			.filter(runtime -> runtimeHasPort(runtime, port))
			.findAny();
	}

	private static boolean runtimeHasPort(HttpServiceRuntime runtime, String port) {

		Map<String, Object> map = runtime.getRuntimeDTO().serviceDTO.properties;
		return port.equals(map.get("org.osgi.service.http.port"));
	}

}
