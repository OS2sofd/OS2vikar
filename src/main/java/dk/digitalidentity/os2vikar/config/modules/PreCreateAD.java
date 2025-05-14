package dk.digitalidentity.os2vikar.config.modules;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PreCreateAD {
	private long createNumberWithLicense = 10;
	private long createNumberWithoutLicense = 5;
}
