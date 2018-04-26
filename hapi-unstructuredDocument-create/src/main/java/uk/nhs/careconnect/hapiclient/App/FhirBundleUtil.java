package uk.nhs.careconnect.hapiclient.App;

import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FhirBundleUtil {

    // THIS IS A LIBRARY CLASS !!!BUT!!! IS CURRENTLY IN SEVERAL MODULES

    // BE CAREFUL AND UPDATE ALL VERSIONS.
    //
    // This will be moved to a central library when development is at that stage

    private Bundle fhirDocument = null;

    private Patient patient = null;

    Map<String,String> referenceMap = new HashMap<>();

    final String uuidtag = "urn:uuid:";

    private static final Logger log = LoggerFactory.getLogger(FhirBundleUtil.class);

    FhirBundleUtil(Bundle.BundleType value) {
        fhirDocument = new Bundle()
                .setType(value);
        fhirDocument.getIdentifier().setValue(UUID.randomUUID().toString()).setSystem("https://tools.ietf.org/html/rfc4122");
    }

    public Bundle getFhirDocument() {
        return fhirDocument;
    }

    public void setFhirDocument(Bundle fhirDocument) {
        this.fhirDocument = fhirDocument;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public void processReferences() {

        for (Bundle.BundleEntryComponent entry : fhirDocument.getEntry()) {

            if (entry.getResource() instanceof AllergyIntolerance) {
                AllergyIntolerance allergyIntolerance = (AllergyIntolerance) entry.getResource();
                allergyIntolerance.setPatient(new Reference(uuidtag+patient.getId()));
            }

            if (entry.getResource() instanceof Condition) {
                Condition condition = (Condition) entry.getResource();
                condition.setSubject(new Reference(uuidtag+patient.getId()));
                if (condition.hasContext()) {
                    condition.setContext(getUUIDReference(condition.getContext()));
                }
            }

            if (entry.getResource() instanceof Composition) {
                Composition composition = (Composition) entry.getResource();

                if (composition.hasSubject()) composition.setSubject(new Reference(uuidtag+composition.getSubject().getReference()));
                for (Composition.CompositionAttesterComponent attester : composition.getAttester()) {
                    if (attester.hasParty()) {
                        attester.setParty(getUUIDReference(attester.getParty()));
                    }
                }
            }
            if (entry.getResource() instanceof DocumentReference) {
                DocumentReference documentReference = (DocumentReference) entry.getResource();
                if (documentReference.hasContent()) {
                    for (DocumentReference.DocumentReferenceContentComponent content : documentReference.getContent())
                    if (content.hasAttachment()) {
                        log.info("Attachment url = "+content.getAttachment().getUrl());
                        content.getAttachment().setUrl(getNewReferenceUri(content.getAttachment().getUrl()));
                    }
                }
                if (documentReference.hasCustodian()) {
                    documentReference.setCustodian(getUUIDReference(documentReference.getCustodian()));
                }
                for (Reference reference : documentReference.getAuthor()) {
                    reference.setReference(getUUIDReference(reference).getReference());
                }
            }
            if (entry.getResource() instanceof Device) {
                Device device = (Device) entry.getResource();
                if (device.hasOwner()) {
                    device.setOwner(getUUIDReference(device.getOwner()));
                }
            }

            if (entry.getResource() instanceof Encounter) {
                Encounter encounter = (Encounter) entry.getResource();
                encounter.setSubject(new Reference(uuidtag+patient.getId()));
                if (encounter.hasServiceProvider()) {
                    encounter.setServiceProvider(getUUIDReference(encounter.getServiceProvider()));
                }
            }

            if (entry.getResource() instanceof Observation) {
                Observation observation = (Observation) entry.getResource();
                observation.setSubject(new Reference(uuidtag+patient.getId()));
                if (observation.hasContext()) {
                    observation.setContext(getUUIDReference(observation.getContext()));
                }

            }

            if (entry.getResource() instanceof MedicationRequest) {
                MedicationRequest medicationRequest = (MedicationRequest) entry.getResource();
                if (medicationRequest.hasContext()) {
                    medicationRequest.setContext(getUUIDReference(medicationRequest.getContext()));
                }
                medicationRequest.setSubject(new Reference(uuidtag+patient.getId()));
            }

            if (entry.getResource() instanceof MedicationStatement) {
                MedicationStatement medicationStatement = (MedicationStatement) entry.getResource();
                medicationStatement.setSubject(new Reference(uuidtag+patient.getId()));
                if (medicationStatement.hasContext()) {
                    medicationStatement.setContext(getUUIDReference(medicationStatement.getContext()));
                }
            }

            if (entry.getResource() instanceof Organization) {
                Organization organization = (Organization) entry.getResource();
                if (organization.hasPartOf()) {
                    if (getNewReferenceUri(organization.getPartOf().getReference())!=null) {
                        organization.setPartOf(getUUIDReference(organization.getPartOf()));
                    } else {
                        organization.setPartOf(null);
                    }
                }
            }

            if (entry.getResource() instanceof Patient) {
                Patient patient = (Patient) entry.getResource();

                patient.setManagingOrganization(new Reference(uuidtag + getNewReferenceUri(patient.getManagingOrganization().getReference())));
                for (Reference reference : patient.getGeneralPractitioner()) {
                    patient.getGeneralPractitioner().get(0).setReference(uuidtag + getNewReferenceUri(reference.getReference()));
                }
            }

            if (entry.getResource() instanceof Practitioner) {
                Practitioner practitioner = (Practitioner) entry.getResource();
            }

            if (entry.getResource() instanceof Procedure) {
                Procedure procedure = (Procedure) entry.getResource();
                procedure.setSubject(new Reference(uuidtag+patient.getId()));
                if (procedure.hasContext()) {
                    procedure.setContext(getUUIDReference(procedure.getContext()));
                }
            }

        }
    }

    public void processBundleResources(Bundle bundle) {
        Practitioner gp = null;
        Organization practice = null;
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            Resource resource = entry.getResource();
            resource.setId(getNewReferenceUri(resource));
            fhirDocument.addEntry().setResource(entry.getResource()).setFullUrl(uuidtag + resource.getId());
            if (entry.getResource() instanceof Patient) {
                patient = (Patient) entry.getResource();
            }
        }
    }

    private Reference getUUIDReference(Reference reference) {
        if (referenceMap.get(reference.getReference()) == null) {
            log.info("Missing refernce "+reference.getReference());
        }
        if (reference.getReference().equals(getNewReferenceUri(getNewReferenceUri(reference.getReference())))) {
            return reference;
        } else {
            return new Reference(uuidtag + getNewReferenceUri(getNewReferenceUri(reference.getReference())));
        }
    }
    private String getNewReferenceUri(Resource resource) {
        return getNewReferenceUri(resource.getResourceType().toString()+"/"+resource.getId());
    }

    private String getNewReferenceUri(String reference) {
        String newReference = referenceMap.get(reference);
        if (newReference != null ) return newReference;
        newReference = UUID.randomUUID().toString();
        referenceMap.put(reference,newReference);
        return newReference;
    }

}
