package org.hl7.fhir.igtools.openapi;

import java.util.List;

import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.ResourceInteractionComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.ResourceVersionPolicy;
import org.hl7.fhir.r4.model.CapabilityStatement.RestfulCapabilityMode;
import org.hl7.fhir.r4.model.CapabilityStatement.TypeRestfulInteraction;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.ContactDetail;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;

public class OpenApiGenerator {

  private CapabilityStatement source;
  private Writer dest;

  public OpenApiGenerator(CapabilityStatement cs, Writer oa) {
    this.source = cs;
    this.dest = oa;
  }

  public void generate(String license) {
    dest.info().title(source.getTitle()).description(source.getDescription()).license(license, null).version(source.getVersion());
    if (source.hasPublisher())
      dest.info().contact(source.getPublisher(), null, null);
    for (ContactDetail cd : source.getContact()) {
      dest.info().contact(cd.getName(), email(cd.getTelecom()), url(cd.getTelecom()));
    }
   
    if (source.hasImplementation()) {
      dest.server(source.getImplementation().getUrl()).description(source.getImplementation().getDescription());
    }
    dest.externalDocs().url(source.getUrl()).description("FHIR CapabilityStatement");
    
    for (CapabilityStatementRestComponent csr : source.getRest()) {
      if (csr.getMode() == RestfulCapabilityMode.SERVER) {
        generatePaths(csr);
      }
    }
  }
  
  private void generatePaths(CapabilityStatementRestComponent csr) {
    for (CapabilityStatementRestResourceComponent r : csr.getResource())
      generateResource(r);
  }
  
  private void generateResource(CapabilityStatementRestResourceComponent r) {
    if (hasOp(r, TypeRestfulInteraction.READ)) {
      generateRead(r);
    }
  }

  private void generateRead(CapabilityStatementRestResourceComponent r) {
    OperationWriter op = makePathResId(r).operation("get");
    op.summary("Read the current state of the resource");
    op.operationId("read"+r.getType());
    opOutcome(op.responses().defaultResponse());
    ResponseObjectWriter resp = op.responses().httpResponse("200");
    resp.description("the resource being returned");
    if (r.getVersioning() != ResourceVersionPolicy.NOVERSION)
      resp.header("ETag").description("Version from Resource.meta.version as a weak ETag");
    if (isJson())
      resp.content("application/fhir+json").schemaRef(specRef()+"/fhir.json.schema/definitions/"+r.getType());
    if (isXml())
      resp.content("application/fhir+xml").schemaRef(specRef()+"/"+r.getType()+".xsd");
  }

  private void opOutcome(ResponseObjectWriter resp) {
    resp.description("Error, with details");
    if (isJson())
      resp.content("application/fhir+json").schemaRef(specRef()+"/fhir.json.schema/definitions/OperationOutcome");
    if (isXml())
      resp.content("application/fhir+xml").schemaRef(specRef()+"/OperationOutcome.xsd");    
  }

  private String specRef() {
    // todo: figure out which version we are running against
    return "http://build.fhir.org";
  }

  private boolean isJson() {
    for (CodeType f : source.getFormat()) {
      if (f.getCode().contains("json"))
        return true;
    }
    return false;
  }

  private boolean isXml() {
    for (CodeType f : source.getFormat()) {
      if (f.getCode().contains("xml"))
        return true;
    }
    return false;
  }

  public PathItemWriter makePathRes(CapabilityStatementRestResourceComponent r) {
    PathItemWriter p = dest.path("/"+r.getType());
    p.summary("Manager for resources of type "+r.getType());
    p.description("The Manager for resources of type "+r.getType()+": provides services to manage the collection of all the "+r.getType()+" instances");
    return p;
  }

  public PathItemWriter makePathResId(CapabilityStatementRestResourceComponent r) {
    PathItemWriter p = dest.path("/"+r.getType()+"/{rid}");
    p.summary("Read/Write/etc resource instance of type "+r.getType());
    p.description("Access to services to manage the state of a single resource of type "+r.getType());
    return p;
  }

  private boolean hasOp(CapabilityStatementRestResourceComponent r, TypeRestfulInteraction opCode) {
    for (ResourceInteractionComponent op : r.getInteraction()) {
      if (op.getCode() == opCode) 
        return true;
    }
    return false;
  }

  private String url(List<ContactPoint> telecom) {
    for (ContactPoint cp : telecom) {
      if (cp.getSystem() == ContactPointSystem.URL)
        return cp.getValue();
    }
    return null;
  }


  private String email(List<ContactPoint> telecom) {
    for (ContactPoint cp : telecom) {
      if (cp.getSystem() == ContactPointSystem.EMAIL)
        return cp.getValue();
    }
    return null;
  }



}
