package com.mlreef.rest.feature.pipeline

import com.mlreef.rest.CodeProject
import com.mlreef.rest.CodeProjectRepository
import com.mlreef.rest.DataOperation
import com.mlreef.rest.DataProcessorInstance
import com.mlreef.rest.DataProcessorRepository
import com.mlreef.rest.DataProject
import com.mlreef.rest.DataProjectRepository
import com.mlreef.rest.DataType
import com.mlreef.rest.FileLocation
import com.mlreef.rest.FileLocationType
import com.mlreef.rest.Person
import com.mlreef.rest.PersonRepository
import com.mlreef.rest.PipelineConfig
import com.mlreef.rest.PipelineConfigRepository
import com.mlreef.rest.PipelineInstanceRepository
import com.mlreef.rest.PipelineStatus
import com.mlreef.rest.ProcessorParameterRepository
import com.mlreef.rest.SubjectRepository
import com.mlreef.rest.VisibilityScope
import com.mlreef.rest.external_api.gitlab.GitlabRestClient
import com.mlreef.rest.external_api.gitlab.dto.Branch
import com.mlreef.rest.external_api.gitlab.dto.Commit
import com.mlreef.rest.service.AbstractServiceTest
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import java.util.UUID.randomUUID

class PipelineServiceTest : AbstractServiceTest() {

    lateinit var service: PipelineService

    @Autowired
    private lateinit var dataProjectRepository: DataProjectRepository

    @Autowired
    private lateinit var pipelineConfigRepository: PipelineConfigRepository

    @Autowired
    private lateinit var pipelineInstanceRepository: PipelineInstanceRepository

    @Autowired
    private lateinit var subjectRepository: SubjectRepository

    @Autowired
    private lateinit var personRepository: PersonRepository

    @Autowired
    private lateinit var dataProcessorRepository: DataProcessorRepository

    @Autowired
    private lateinit var codeProjectRepository: CodeProjectRepository

    @Autowired
    private lateinit var processorParameterRepository: ProcessorParameterRepository

    @MockkBean
    private lateinit var restClient: GitlabRestClient

    private var ownerId: UUID = randomUUID()
    private var dataRepositoryId: UUID = randomUUID()
    private var dataRepositoryId2: UUID = randomUUID()

    @BeforeEach
    fun prepare() {
        service = PipelineService(
            pipelineConfigRepository = pipelineConfigRepository,
            pipelineInstanceRepository = pipelineInstanceRepository,
            subjectRepository = subjectRepository,
            dataProjectRepository = dataProjectRepository,
            dataProcessorRepository = dataProcessorRepository,
            processorParameterRepository = processorParameterRepository,
            gitlabRootUrl = "http://localhost:10080",
            gitlabRestClient = restClient,
            epfBackendUrl = "epfBackendUrl.com",
            epfGitlabUrl = "gitlab:10080",
            epfImageTag = "latest")

        val subject = Person(ownerId, "new-person", "person's name", 1L)
        subjectRepository.save(subject)
        val dataRepository = DataProject(dataRepositoryId, "new-repo", "url", "Test DataProject", subject.id, "mlreef", "project", "group/project", 0, VisibilityScope.PUBLIC, arrayListOf())
        dataProjectRepository.save(DataProject(dataRepositoryId2, "new-repo2", "url", "Test DataProject", subject.id, "mlreef", "project", "group/project", 0, VisibilityScope.PUBLIC, arrayListOf()))

        dataProjectRepository.save(dataRepository)
    }

    @Test
    fun `Cannot create PipelineConfig for missing Owner`() {
        assertThrows<IllegalArgumentException> {
            service.createPipelineConfig(
                randomUUID(),
                dataRepositoryId,
                "DATA",
                "name",
                "sourcebranch",
                listOf(), listOf())
        }
    }

    @Test
    fun `Cannot create PipelineConfig for missing DataProject`() {
        assertThrows<IllegalArgumentException> {
            service.createPipelineConfig(
                ownerId,
                randomUUID(),
                "DATA",
                "name",
                "sourcebranch",
                listOf(), listOf())
        }
    }

    @Test
    fun `Cannot create PipelineConfig for missing branch name`() {
        assertThrows<IllegalArgumentException> {
            service.createPipelineConfig(
                ownerId,
                dataRepositoryId,
                "DATA",
                "name",
                "",
                listOf(), listOf())
        }
    }

    @Test
    fun `Cannot create PipelineConfig for missing slug`() {
        assertThrows<IllegalArgumentException> {
            service.createPipelineConfig(
                ownerId,
                dataRepositoryId,
                "DATA",
                "name",
                "",
                listOf(), listOf())
        }
    }

    @Test
    fun `Cannot create PipelineConfig for missing pipelineType`() {
        assertThrows<IllegalArgumentException> {
            service.createPipelineConfig(
                ownerId,
                dataRepositoryId,
                "",

                "name",
                "sourcebranch",
                listOf(), listOf())
        }
    }

    @Test
    fun `Cannot create PipelineConfig for invalid pipelineType`() {
        assertThrows<IllegalArgumentException> {
            service.createPipelineConfig(
                ownerId,
                dataRepositoryId,
                "DATEN",
                "name",
                "sourcebranch",
                listOf(), listOf())
        }
    }

    @Test
    fun `Cannot create PipelineConfig for duplicate slug scoped to DataProject`() {
        service.createPipelineConfig(
            ownerId,
            dataRepositoryId,
            "DATA",
            "name",
            "source",
            listOf(), listOf())
        assertThrows<IllegalArgumentException> {
            service.createPipelineConfig(
                ownerId,
                dataRepositoryId,
                "DATA",
                "name",
                "source",
                listOf(), listOf())
        }
    }

    @Test
    fun `Can create PipelineConfig if Owner and DataProject exist`() {
        val createExperiment = service.createPipelineConfig(
            ownerId,
            dataRepositoryId,
            "DATA",
            "name",
            "sourcebranch",
            listOf(), listOf())

        assertThat(createExperiment).isNotNull()
    }

    @Test
    fun `Can create PipelineConfig with reused slug scoped to different DataProject`() {
        service.createPipelineConfig(
            ownerId,
            dataRepositoryId,
            "DATA",
            "name",
            "sourcebranch",
            listOf(), listOf())

        val createExperiment = service.createPipelineConfig(
            ownerId,
            dataRepositoryId2,
            "DATA",
            "name",
            "sourcebranch",
            listOf(), listOf())

        assertThat(createExperiment).isNotNull()
    }

    @Test
    fun `Can create PipelineConfig with different slug scoped same DataProject`() {
        service.createPipelineConfig(
            ownerId,
            dataRepositoryId,
            "DATA",
            "name",
            "sourcebranch",
            listOf(), listOf())

        val createExperiment = service.createPipelineConfig(
            ownerId,
            dataRepositoryId,
            "DATA",
            "another-name",
            "sourcebranch",
            listOf(), listOf())

        assertThat(createExperiment).isNotNull()
    }

    @Test
    fun `Can create PipelineConfig for pipelineType DATA`() {
        val createExperiment = service.createPipelineConfig(
            ownerId,
            dataRepositoryId,
            "DATA",
            "name",
            "sourcebranch",
            listOf(), listOf())

        assertThat(createExperiment).isNotNull()
    }

    @Test
    fun `Can create PipelineConfig with nullable and therefore generated name`() {
        val createExperiment = service.createPipelineConfig(
            ownerId,
            dataRepositoryId,
            "DATA",
            "",
            "sourcebranch",
            listOf(), listOf())

        assertThat(createExperiment).isNotNull()
        assertThat(createExperiment.name).isNotEmpty()
    }

    @Test
    fun `Can create PipelineConfig for pipelineType VISUAL`() {
        val createExperiment = service.createPipelineConfig(
            ownerId,
            dataRepositoryId,
            "VISUAL",
            "name",
            "sourcebranch",
            listOf(), listOf())

        assertThat(createExperiment).isNotNull()
    }

    @Test
    fun `Can create PipelineConfig for pipelineType VISUALisation`() {
        val createExperiment = service.createPipelineConfig(
            ownerId,
            dataRepositoryId,
            "VISUALISATION",
            "name",
            "sourcebranch",
            listOf(), listOf())

        assertThat(createExperiment).isNotNull()
    }

    @Test
    fun `Can create PipelineConfig with empty targetBranchPattern`() {
        val createExperiment = service.createPipelineConfig(
            ownerId,
            dataRepositoryId,
            "DATA",
            "name",
            "sourcebranch",
            listOf(), listOf())

        assertThat(createExperiment).isNotNull()
    }

    @Test
    fun `Can create PipelineConfig with DataProcessors`() {
        val pipelineConfig = createFullMockData()

        assertThat(pipelineConfig).isNotNull()
    }

    @Test
    fun `Can create DataInstance from PipelineConfig`() {
        val pipelineConfig = createFullMockData()

        val createdInstance = pipelineConfig.createInstance(1)

        assertThat(createdInstance).isNotNull()
        assertThat(createdInstance.status).isEqualTo(PipelineStatus.CREATED)
    }

    @Test
    fun `Can create DataInstance from PipelineConfig as deep copy`() {
        val pipelineConfig = createFullMockData()

        val createdInstance = pipelineConfig.createInstance(1)

        assertThat(createdInstance.dataProjectId).isEqualTo(pipelineConfig.dataProjectId)
        assertThat(createdInstance.pipelineConfigId).isEqualTo(pipelineConfig.id)
        assertThat(createdInstance.sourceBranch).isEqualTo(pipelineConfig.sourceBranch)
        assertThat(createdInstance.name).isEqualTo(pipelineConfig.name)
        assertThat(createdInstance.number).isEqualTo(1)
        assertThat(createdInstance.slug).isEqualTo("${pipelineConfig.slug}-${createdInstance.number}")

        assertThat(createdInstance.inputFiles.size).isEqualTo(pipelineConfig.inputFiles.size)
        assertThat(createdInstance.dataOperations.size).isEqualTo(pipelineConfig.dataOperations.size)

        createdInstance.dataOperations.forEachIndexed { index, newInstance ->
            val oldInstance = pipelineConfig.dataOperations[index]
            assertThat(newInstance.slug).isEqualTo(oldInstance.slug)
            assertThat(oldInstance.pipelineConfigId).isEqualTo(pipelineConfig.id)
            assertThat(newInstance.pipelineConfigId).isEqualTo(null)
            assertThat(oldInstance.dataInstanceId).isEqualTo(null)
            assertThat(newInstance.dataInstanceId).isEqualTo(createdInstance.id)
            assertThat(oldInstance.experimentProcessingId).isEqualTo(null)
            assertThat(newInstance.experimentProcessingId).isEqualTo(null)
            assertThat(newInstance.experimentPostProcessingId).isEqualTo(null)
            assertThat(newInstance.experimentPreProcessingId).isEqualTo(null)
        }

        createdInstance.inputFiles.forEachIndexed { index, newInstance ->
            val oldInstance = pipelineConfig.inputFiles[index]

            assertThat(newInstance.location).isEqualTo(oldInstance.location)
            assertThat(newInstance.locationType).isEqualTo(oldInstance.locationType)
        }
    }

    @Test
    fun `Can create DataInstance from PipelineConfig with useful targetBranchPattern`() {
        val testId = randomUUID()
        val createFullMockData1 = createFullMockData("slug1")
        assertThat(createFullMockData1.createTargetBranchName(testId, 1)).isEqualTo("data-pipeline/slug1-1")
        assertThat(createFullMockData("slug2").createTargetBranchName(testId, 3)).isEqualTo("data-pipeline/slug2-3")
        assertThat(createFullMockData("slug3").createTargetBranchName(testId, 8)).isEqualTo("data-pipeline/slug3-8")
    }

    @Test
    fun `Can commit mlreef file to gitlab`() {
        val userToken = "userToken"
        val projectId = 1L
        val targetBranch = "targetBranch"
        val fileContent = "fileContent"
        val sourceBranch = "master"

        val fileContents: Map<String, String> = mapOf(Pair(".mlreef.yml", fileContent))

        every {
            restClient.createBranch(userToken, projectId, targetBranch, sourceBranch)
        } returns (Branch(ref = sourceBranch, branch = targetBranch))
        every {
            restClient.commitFiles(
                token = userToken, targetBranch = targetBranch,
                fileContents = fileContents, projectId = projectId, commitMessage = any(),
                action = "create")
        } returns (Commit())

        val commit = service.commitYamlFile(userToken, projectId, targetBranch, fileContent, sourceBranch)

        verify { restClient.createBranch(userToken, projectId, targetBranch, sourceBranch) }
        verify { restClient.commitFiles(userToken, projectId, targetBranch, any(), fileContents, action = "create") }

        assertThat(commit).isNotNull()
    }

    private fun createFullMockData(name: String = "name"): PipelineConfig {
        val author = Person(randomUUID(), "person", "name", 1L)
        val codeProjectId = randomUUID()

        personRepository.save(author)
        codeProjectRepository.save(CodeProject(id = codeProjectId, slug = "code-project-augment", name = "CodeProject Augment", ownerId = author.id, url = "url",
            gitlabGroup = "", gitlabId = 0, gitlabProject = ""))

        val dataOp1 = DataOperation(
            id = randomUUID(), slug = "commons-augment", name = "Augment",
            command = "augment", inputDataType = DataType.IMAGE, outputDataType = DataType.IMAGE,
            visibilityScope = VisibilityScope.PUBLIC, author = author,
            description = "description",
            codeProjectId = codeProjectId)
        dataProcessorRepository.save(dataOp1)
        val createPipelineConfig = service.createPipelineConfig(
            ownerId,
            dataRepositoryId,
            "DATA",
            name,
            "sourcebranch",
            listOf(), listOf()
        )
        createPipelineConfig.addProcessor(DataProcessorInstance(id = randomUUID(), dataProcessor = dataOp1))
        createPipelineConfig.addInputFile(FileLocation(randomUUID(), FileLocationType.PATH, "/path"))
        createPipelineConfig.addInputFile(FileLocation(randomUUID(), FileLocationType.PATH, "/path2"))
        return pipelineConfigRepository.save(createPipelineConfig)
    }
}
