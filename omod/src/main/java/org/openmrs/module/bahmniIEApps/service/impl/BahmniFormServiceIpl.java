package org.openmrs.module.bahmniIEApps.service.impl;

import org.apache.commons.collections.CollectionUtils;
import org.openmrs.Form;
import org.openmrs.FormResource;
import org.openmrs.api.APIException;
import org.openmrs.api.FormService;
import org.openmrs.module.bahmniIEApps.dao.BahmniFormDao;
import org.openmrs.module.bahmniIEApps.mapper.BahmniFormMapper;
import org.openmrs.module.bahmniIEApps.model.BahmniForm;
import org.openmrs.module.bahmniIEApps.model.BahmniFormResource;
import org.openmrs.module.bahmniIEApps.service.BahmniFormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BahmniFormServiceIpl implements BahmniFormService {
    private FormService formService;
    private BahmniFormDao bahmniFormDao;
    private final String DEFAULT_VERSION = "1";
    private final String MULTIPLE_DRAFT_EXCEPTION = "Form cannot have more than one drafts.";

    @Autowired
    public BahmniFormServiceIpl(FormService formService, BahmniFormDao bahmniFormDao) {
        this.formService = formService;
        this.bahmniFormDao = bahmniFormDao;
    }

    @Override
    @Transactional
    public BahmniFormResource saveFormResource(BahmniFormResource bahmniFormResource) {
        Form form = formService.getFormByUuid(bahmniFormResource.getForm().getUuid());
        FormResource formResource = getFormResource(bahmniFormResource.getUuid());
        if(form.getPublished()) {
            form = cloneForm(form);
            form.setVersion(incrementVersion(form.getName()));
            formService.saveForm(form);

            formResource = cloneFormResource(formResource);
        }
        formResource.setForm(form);
        formResource.setName(bahmniFormResource.getForm().getName());
        formResource.setDatatypeClassname(bahmniFormResource.getDataType());
        formResource.setValueReferenceInternal(bahmniFormResource.getValueReference());
        formResource = formService.saveFormResource(formResource);
        return new BahmniFormMapper().map(formResource);
    }

    @Override
    public BahmniForm publish(String formUuid) {
        Form form = formService.getFormByUuid(formUuid);
        if (form != null) {
            form.setPublished(Boolean.TRUE);
            formService.saveForm(form);
        }
        return new BahmniFormMapper().map(form);
    }

    @Override
    public List<BahmniForm> getAllForms(boolean includeRetired) {
        List<Form> forms = bahmniFormDao.getAllPublishedForms(includeRetired);
        return getLatestFormByVersion(forms);
    }

    private List<BahmniForm> getLatestFormByVersion(List<Form> forms) {
        Set<String> bahmniFormNames = new HashSet<>();
        List<BahmniForm> bahmniForms = new ArrayList<>();
        BahmniFormMapper mapper = new BahmniFormMapper();
        for(Form form : forms) {
            if (!bahmniFormNames.contains(form.getName())) {
                bahmniFormNames.add(form.getName());
                bahmniForms.add(mapper.map(form));
            }
        }
        return bahmniForms;
    }

    private FormResource getFormResource(String formResourceUuid) {
        FormResource formResource = formService.getFormResourceByUuid(formResourceUuid);
        if(null == formResource) {
            formResource = new FormResource();
        }
        return formResource;
    }

    private Form cloneForm(Form form) {
        Form clonedForm = new Form();
        clonedForm.setName(form.getName());
        clonedForm.setVersion(form.getVersion());
        clonedForm.setBuild(form.getBuild());
        clonedForm.setEncounterType(form.getEncounterType());
        clonedForm.setCreator(form.getCreator());
        return clonedForm;
    }

    private FormResource cloneFormResource(FormResource formResource) {
        FormResource clonedFormResource = new FormResource();
        clonedFormResource.setName(formResource.getName());
        clonedFormResource.setValueReferenceInternal(formResource.getValueReference());
        clonedFormResource.setDatatypeClassname(formResource.getDatatypeClassname());
        clonedFormResource.setDatatypeConfig(formResource.getDatatypeConfig());
        clonedFormResource.setPreferredHandlerClassname(formResource.getPreferredHandlerClassname());
        clonedFormResource.setHandlerConfig(formResource.getHandlerConfig());
        return clonedFormResource;
    }

    /**
     * This will throw APIException.class exception if there is more than one draft version.
     * @param formName
     */
    private void validateForMultipleDraft(String formName) {
        List<Form> forms = bahmniFormDao.getDraftFormByName(formName);
        if(CollectionUtils.isNotEmpty(forms) && forms.size() > 1) {
            throw new APIException(MULTIPLE_DRAFT_EXCEPTION);
        }
    }

    private String incrementVersion(String formName) {
        Form form = formService.getForm(formName);
        if(null != form) {
            Float version = Float.parseFloat(form.getVersion());
            version++;
            return version.toString();
        }
        return DEFAULT_VERSION;
    }
}