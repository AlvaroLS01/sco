package com.comerzzia.brico.pos.util.format;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.internal.constraintvalidators.EmailValidator;
import com.comerzzia.pos.util.i18n.I18N;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;

public class BricoEmailValidator extends EmailValidator {

	// email validator SCO
	private static final long serialVersionUID = 1L;
	private static final String LOCAL_PART_REGEX = "^[A-Za-z0-9]+(?:\\.[A-Za-z0-9]+)*$";
	private static final String DOMAIN_LABEL_REGEX = "^[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*$";
	private static final String DOMAIN_REGEX = "^(?=.{1,255}$)(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,}$";
	private static final BricoEmailValidator INSTANCE = new BricoEmailValidator();

	private BricoEmailValidator() {
		super();
	}

        public static String getValidationErrorKey(String email) {
                if (StringUtils.isBlank(email) || email.contains(" ")) {
                        return I18N.getTexto("El formato del email no es válido");
                }

                if (!email.contains("@")) {
                        return I18N.getTexto("El email debe contener '@'");
                }

                boolean basicOk = email.indexOf('@') > 0 && email.indexOf('@') == email.lastIndexOf('@');
                if (!basicOk) {
                        return I18N.getTexto("El formato del email no es válido");
                }

		String[] parts = email.split("@", 2);
		String localPart = parts[0];
		String domainPart = parts[1];

		boolean advancedOk = localPart.length() <= 64 && domainPart.length() <= 255 && localPart.matches(LOCAL_PART_REGEX) && domainPart.matches(DOMAIN_REGEX)
		        && Arrays.stream(domainPart.split("\\.")).allMatch(label -> label.matches(DOMAIN_LABEL_REGEX)) && INSTANCE.isValid(email, (ConstraintValidatorContext) null);
		if (!advancedOk) {
			return I18N.getTexto("El formato del email no es válido");
		}

		return null;
	}

	public static boolean isValidEmail(String email) {
		return getValidationErrorKey(email) == null;
	}

	@Override
	public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
		return super.isValid(value, context);
	}
}
