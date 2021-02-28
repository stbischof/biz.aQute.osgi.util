package biz.aQute.gogo.commands.provider.jaxrs;

import java.util.List;
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
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.runtime.dto.RuntimeDTO;

import biz.aQute.gogo.commands.dtoformatter.DTOFormatter;

@Component(service = Object.class, immediate = true)
@GogoCommand(scope = "dto", function = {
	"jaxrsruntimes", "jaxrsruntime"
})
public class JaxRS {

	@Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
	volatile List<JaxrsServiceRuntime>	serviceRuntimes;

	@Reference(cardinality = ReferenceCardinality.MANDATORY, policyOption = ReferencePolicyOption.GREEDY)
	JaxrsServiceRuntime					defaultServiceRuntime;

	@Reference
	DTOFormatter						formatter;

	@Activate
	void activate() {
		// todo: formatter

	}

	@Descriptor("Show the RuntimeDTO of the HttpServiceRuntime")
	public List<RuntimeDTO> jaxrsruntimes() throws InterruptedException {

		return serviceRuntimes.stream()
			.map(JaxrsServiceRuntime::getRuntimeDTO)
			.collect(Collectors.toList());
	}

	@Descriptor("Show the RuntimeDTO of the JaxrsServiceRuntime")
	public RuntimeDTO jaxrsruntime(@Descriptor("service.id")
	@Parameter(absentValue = "-1", names = "-s")
	long service_id) throws InterruptedException {

		return runtime(service_id).map(JaxrsServiceRuntime::getRuntimeDTO)
			.orElse(null);
	}

	private Optional<JaxrsServiceRuntime> runtime(long service_id) {

		if (service_id < 0) {
			return Optional.ofNullable(defaultServiceRuntime);
		}
		return serviceRuntimes.stream()
			.filter(runtime -> runtimeHasPort(runtime, service_id))
			.findAny();
	}

	private static boolean runtimeHasPort(JaxrsServiceRuntime runtime, long service_id) {

		return service_id == runtime.getRuntimeDTO().serviceDTO.id;
	}

}
