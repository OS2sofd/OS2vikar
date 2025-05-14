package dk.digitalidentity.os2vikar.interceptors;

import java.lang.reflect.Method;
import java.util.Objects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dk.digitalidentity.os2vikar.dao.model.Substitute;
import dk.digitalidentity.os2vikar.service.AuditLogService;
import dk.digitalidentity.os2vikar.service.SubstituteService;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class LogInterceptor {

	@Autowired
	private AuditLogService auditLogService;

	public ThreadLocal<Substitute> substituteHolder = new ThreadLocal<>();

	@Around(value = "execution(* dk.digitalidentity.os2vikar.controller..*.*(..)) && @annotation(AuditLogIntercepted)")
	public Object interceptBefore(ProceedingJoinPoint joinPoint) {
		Object response = null;
		try {
			MethodSignature signature = (MethodSignature) joinPoint.getSignature();
			Method method = signature.getMethod();
			AuditLogIntercepted annotation = method.getAnnotation(AuditLogIntercepted.class);
			String operation = annotation.operation();
			String[] args = annotation.args();
			Object[] actualArgs = joinPoint.getArgs();
			String[] methodParams = signature.getParameterNames();

			StringBuilder builder = new StringBuilder();
			for (String arg : args) {
				boolean found = false;
				Object obj = null;

				for (int i = 0; i < methodParams.length; i++) {
					if (Objects.equals(arg, methodParams[i])) {
						obj = actualArgs[i];
						found = true;
						break;
					}
				}

				if (found) {
					if (obj != null) {
						if (builder.length() > 0) {
							builder.append(",\n");
						}

						String value = obj.toString();
						if ("cpr".equals(arg)) {
							value = SubstituteService.maskCpr(value);
						}

						builder.append(arg + ": " + value);
					}
				}
				else {
					log.error("No argument with the given name " + arg + " supplied for operation " + operation);
				}
			}

			response = joinPoint.proceed();
			auditLogService.log(operation, builder.toString(), substituteHolder.get());

		} catch (Throwable e) {
			log.warn("Failed to proceed method annotated with @AuditLogIntercepted. Message: " + e.getMessage(), e);
		} finally {
			substituteHolder.remove();
		}
		
		return response;
	}
}
