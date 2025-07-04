package dk.digitalidentity.os2vikar.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
public class FKOU {
	private String uuid;
	private String name;
	private String parentOU;
}
