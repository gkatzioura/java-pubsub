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

import static com.google.cloud.pubsub.maven.TestUtils.AVRO_SCHEMA_DEFINITION;
import static com.google.cloud.pubsub.maven.TestUtils.PROTO_SCHEMA_DEFINITION;
import static com.google.cloud.pubsub.maven.TestUtils.TEST_AVRO_SCHEMA;
import static com.google.cloud.pubsub.maven.TestUtils.TEST_PROTO_SCHEMA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.pubsub.v1.Schema;
import com.google.pubsub.v1.Schema.Type;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LocalSchemaStorageTest {

  private LocalSchemaStorage localSchemaStorage;

  @Before
  public void setUp() {
    new File("./test-project").mkdirs();
    localSchemaStorage = new LocalSchemaStorage(new File("./"));
  }

  @After
  public void tearDown() {
    new File("./test-project").delete();
  }

  @Test
  public void shouldThrowExceptionOnInvalidPath() {
    Schema schema = mock(Schema.class);
    when(schema.getType()).thenReturn(Type.TYPE_UNSPECIFIED);
    assertThrows(IllegalArgumentException.class, () -> localSchemaStorage.save(schema));
  }

  @Test
  public void shouldStoreAvroSchema() throws IOException {
    Schema schema = TEST_AVRO_SCHEMA;

    SchemaRepository schemaRepository = mock(SchemaRepository.class);
    when(schemaRepository.fetch("an-avro-schema-name@f81ba5ff")).thenReturn(schema);

    File stored = localSchemaStorage.save(schema);
    byte[] bytes = Files.readAllBytes(stored.toPath());
    assertEquals(AVRO_SCHEMA_DEFINITION, new String(bytes));
  }

  @Test
  public void shouldStoreProtoSchema() throws IOException {
    Schema schema = TEST_PROTO_SCHEMA;

    SchemaRepository schemaRepository = mock(SchemaRepository.class);
    when(schemaRepository.fetch("proto-schema-name@f81ba5ff")).thenReturn(schema);

    File stored = localSchemaStorage.save(schema);
    byte[] bytes = Files.readAllBytes(stored.toPath());
    assertEquals(PROTO_SCHEMA_DEFINITION, new String(bytes));
  }

  @Test
  public void shouldCreatePathWithoutVersion() {
    Schema schema = mock(Schema.class);

    when(schema.getName()).thenReturn("projects/test-project/schemas/1234@1234");
    when(schema.getType()).thenReturn(Type.AVRO);

    String path = localSchemaStorage.location(schema);
    assertEquals("./test-project/1234.avsc",path);
  }

  @Test
  public void shouldCreatePathForAvroRecord() {
    Schema schema = mock(Schema.class);

    when(schema.getName()).thenReturn("projects/test-project/schemas/1234");
    when(schema.getType()).thenReturn(Type.AVRO);

    String path = localSchemaStorage.location(schema);
    assertEquals("./test-project/1234.avsc",path);
  }

  @Test
  public void shouldCreatePathForProtoRecord() {
    Schema schema = mock(Schema.class);
    when(schema.getName()).thenReturn("projects/test-project/schemas/1234");
    when(schema.getType()).thenReturn(Type.PROTOCOL_BUFFER);

    String path = localSchemaStorage.location(schema);
    assertEquals("./test-project/1234.proto",path);
  }

  @Test
  public void shouldFailIfDirectoryIsAFile() {
    String filePath = LocalSchemaStorage.class.getClassLoader().getResource("non-directory.txt").getFile();
    Log log = mock(Log.class);
    File file = new File(filePath);

    assertThrows(
        MojoExecutionException.class, () -> LocalSchemaStorage.create(log, file,"project-id"));
  }

  @Test
  public void shouldCreateProjectSubPath() throws MojoExecutionException {
    String filePath = LocalSchemaStorage.class
        .getClassLoader()
        .getResource("non-directory.txt")
        .getFile();

    Log log = mock(Log.class);
    File file = new File(filePath).getParentFile();

    LocalSchemaStorage localSchemaStorage = LocalSchemaStorage.create(log, file, "project-id");
    assertNotNull(localSchemaStorage);

    boolean projectSubDirectoryExists = new File(file.getPath()+"/project-id").isDirectory();
    assertTrue(projectSubDirectoryExists);
  }

}