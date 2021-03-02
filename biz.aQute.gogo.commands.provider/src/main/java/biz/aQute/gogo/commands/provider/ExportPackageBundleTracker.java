package biz.aQute.gogo.commands.provider;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTracker;

public class ExportPackageBundleTracker extends BundleTracker<Bundle> implements Closeable {

	private static String						LOG						= "org.osgi.service.log";
	private static String						LOG_ADMIN				= "org.osgi.service.log.admin";
	private static String						COMPONENT_RUNTIME		= "org.osgi.service.component.runtime";
	private static String						COMPONENT_RUNTIME_DTO	= "org.osgi.service.component.runtime.dto";
	private static String						HTTP_RUNTIME			= "org.osgi.service.http.runtime";
	private static String						HTTP_RUNTIME_DTO		= "org.osgi.service.http.runtime.dto";
	private static String						JAXRS_RUNTIME			= "org.osgi.service.jaxrs.runtime";
	private static String						JAXRS_RUNTIME_DTO		= "org.osgi.service.jaxrs.runtime.dto";

	private Activator							activator				= new Activator();
	private Map<Bundle, ServiceRegistration<?>>	reg						= new ConcurrentHashMap<>();

	public ExportPackageBundleTracker(BundleContext context, Activator activator) {
		super(context, Bundle.ACTIVE | Bundle.UNINSTALLED, null);
		this.activator = activator;
	}

	@Override
	public Bundle addingBundle(Bundle bundle, BundleEvent event) {

		// LOG
		if (exports(bundle, LOG, LOG_ADMIN)) {
			try {
				ServiceRegistration<?> srLOG = activator.register(LoggerAdminCommands.class);
				reg.put(bundle, srLOG);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// SCR
		if (exports(bundle, COMPONENT_RUNTIME, COMPONENT_RUNTIME_DTO)) {
			try {
				ServiceRegistration<?> srSCR = activator.register(DS.class);
				reg.put(bundle, srSCR);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// HTTP
		if (exports(bundle, HTTP_RUNTIME, HTTP_RUNTIME_DTO)) {
			try {
				ServiceRegistration<?> srHTTP = activator.register(HTTP.class);
				reg.put(bundle, srHTTP);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// JAXRS
		if (exports(bundle, JAXRS_RUNTIME, JAXRS_RUNTIME_DTO)) {
			try {
				ServiceRegistration<?> srJaxRs = activator.register(JaxRS.class);
				reg.put(bundle, srJaxRs);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return super.addingBundle(bundle, event);
	}

	private boolean exports(Bundle bundle, String... imports) {

		String exportedPackages = bundle.getHeaders()
			.get(Constants.EXPORT_PACKAGE);

		exportedPackages = removeUsesSegment(exportedPackages);
		for (String imp : imports) {
			if (!exportedPackages.contains(imp)) {
				return false;
			}
		}
		return true;
	}

	private String removeUsesSegment(String exportedPackages) {
		return exportedPackages.replaceAll("uses:=\"[\\w\\.,]*\"", "");
	}

	@Override
	public void remove(Bundle bundle) {

		ServiceRegistration<?> sr = reg.get(bundle);
		if (sr != null) {
			sr.unregister();
		}
		super.remove(bundle);
	}

	@Override
	public void close() {
		reg.values()
			.forEach(ServiceRegistration::unregister);
		super.close();

	}

}
