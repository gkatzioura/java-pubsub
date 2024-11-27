/*
 *  Copyright 2024 Emmanouil Gkatziouras
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.pubsub.maven;

import static com.google.cloud.pubsub.maven.TestUtils.PROJECT_NAME;
import static com.google.cloud.pubsub.maven.TestUtils.TEST_AVRO_SCHEMA;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.pubsub.v1.SchemaServiceClient;
import com.google.cloud.pubsub.v1.SchemaServiceClient.ListSchemasPagedResponse;
import com.google.pubsub.v1.ProjectName;
import com.google.pubsub.v1.Schema;
import com.google.pubsub.v1.SchemaName;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DownloadSchemaRegistryMojoTest {

  @Before
  public void setUp() {
    new File("./test-project").mkdirs();
  }

  @After
  public void tearDown() {
    new File("./test-project").delete();
  }

  @Test
  public void shouldDownloadFiles() throws MojoExecutionException, MojoFailureException {
    SchemaServiceClient schemaServiceClient = mock(SchemaServiceClient.class);

    DownloadSchemaRegistryMojo downloadSchemaRegistryMojo = DownloadSchemaRegistryMojo.create(
        schemaServiceClient,
        PROJECT_NAME,
        new File("./"),
        null,
        false);

    ListSchemasPagedResponse response = listSchemasPagedResponse(TEST_AVRO_SCHEMA);
    when(schemaServiceClient.listSchemas(any(ProjectName.class))).thenReturn(response);
    when(schemaServiceClient.getSchema(any(SchemaName.class))).thenReturn(TEST_AVRO_SCHEMA);

    downloadSchemaRegistryMojo.execute();

    File file = new File("./test-project/an-avro-schema-name.avsc");
    assertTrue(file.exists());
  }

  @Test
  public void shouldDownloadOnlyAvroFiles() throws MojoExecutionException, MojoFailureException {
    SchemaServiceClient schemaServiceClient = mock(SchemaServiceClient.class);

    DownloadSchemaRegistryMojo downloadSchemaRegistryMojo = DownloadSchemaRegistryMojo.create(
        schemaServiceClient,
        PROJECT_NAME,
        new File("./"),
        "AVRO",
        false);

    ListSchemasPagedResponse response = listSchemasPagedResponse(TEST_AVRO_SCHEMA);
    when(schemaServiceClient.listSchemas(any(ProjectName.class))).thenReturn(response);
    when(schemaServiceClient.getSchema(any(SchemaName.class))).thenReturn(TEST_AVRO_SCHEMA);

    downloadSchemaRegistryMojo.execute();

    File file = new File("./test-project/an-avro-schema-name.avsc");
    assertTrue(file.exists());
  }

  @Test
  public void shouldNotDownloadOtherTypeSpecified()
      throws MojoExecutionException, MojoFailureException {
    SchemaServiceClient schemaServiceClient = mock(SchemaServiceClient.class);

    DownloadSchemaRegistryMojo downloadSchemaRegistryMojo = DownloadSchemaRegistryMojo.create(
        schemaServiceClient,
        PROJECT_NAME,
        new File("./"),
        "PROTOCOL_BUFFER",
        false);

    ListSchemasPagedResponse response = listSchemasPagedResponse(TEST_AVRO_SCHEMA);
    when(schemaServiceClient.listSchemas(any(ProjectName.class))).thenReturn(response);

    downloadSchemaRegistryMojo.execute();

    verify(schemaServiceClient, times(0)).listSchemas(anyString());
  }

  @Test
  public void shouldThrowExceptionOnGCPError() throws MojoExecutionException, MojoFailureException {
    SchemaServiceClient schemaServiceClient = mock(SchemaServiceClient.class);

    DownloadSchemaRegistryMojo downloadSchemaRegistryMojo = DownloadSchemaRegistryMojo.create(
        schemaServiceClient,
        PROJECT_NAME,
        new File("./"),
        null,
        false);

    ListSchemasPagedResponse response = listSchemasPagedResponse(TEST_AVRO_SCHEMA);
    when(schemaServiceClient.listSchemas(any(ProjectName.class))).thenReturn(response);
    when(schemaServiceClient.getSchema((any(SchemaName.class)))).thenThrow(
        new StatusRuntimeException(Status.NOT_FOUND));

    assertThrows(MojoExecutionException.class, () -> downloadSchemaRegistryMojo.execute());
  }

  @Test
  public void throwExceptionOnPubSubError() throws MojoExecutionException, MojoFailureException {
    SchemaServiceClient schemaServiceClient = mock(SchemaServiceClient.class);

    DownloadSchemaRegistryMojo downloadSchemaRegistryMojo = DownloadSchemaRegistryMojo.create(
        schemaServiceClient,
        PROJECT_NAME,
        new File("./"),
        null,
        false);

    ListSchemasPagedResponse response = listSchemasPagedResponse(TEST_AVRO_SCHEMA);
    when(schemaServiceClient.listSchemas(any(ProjectName.class))).thenReturn(response);
    when(schemaServiceClient.getSchema(any(SchemaName.class))).thenReturn(TEST_AVRO_SCHEMA);

    downloadSchemaRegistryMojo.execute();

    File file = new File("./test-project/an-avro-schema-name.avsc");
    assertTrue(file.exists());
  }


  @Test
  public void shouldNotExecuteIfSkipped() throws MojoExecutionException, MojoFailureException {
    SchemaServiceClient schemaServiceClient = mock(SchemaServiceClient.class);
    DownloadSchemaRegistryMojo downloadSchemaRegistryMojo = spy(DownloadSchemaRegistryMojo.create(
        schemaServiceClient,
        PROJECT_NAME,
        null,
        null,
        true));

    downloadSchemaRegistryMojo.execute();
    verify(schemaServiceClient, times(0)).listSchemas(anyString());
    verify(downloadSchemaRegistryMojo, times(1)).getLog();
  }

  @Test
  public void shouldCloseClientWhenCallingClose() throws IOException {
    SchemaServiceClient schemaServiceClient = mock(SchemaServiceClient.class);
    DownloadSchemaRegistryMojo downloadSchemaRegistryMojo = DownloadSchemaRegistryMojo.create(
        schemaServiceClient,
        PROJECT_NAME,
        null,
        null,
        false);

    downloadSchemaRegistryMojo.close();
    verify(schemaServiceClient, times(1)).close();
  }

  private static ListSchemasPagedResponse listSchemasPagedResponse(Schema schema) {
    List<Schema> schemas = new ArrayList<>();
    schemas.add(schema);
    ListSchemasPagedResponse listSchemasPagedResponse = mock(ListSchemasPagedResponse.class);
    when(listSchemasPagedResponse.iterateAll()).thenReturn(schemas);
    return listSchemasPagedResponse;
  }

}