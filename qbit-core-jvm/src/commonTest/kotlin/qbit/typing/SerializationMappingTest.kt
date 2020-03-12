package qbit.typing

import kotlinx.serialization.modules.plus
import qbit.factorization.KSFactorization
import qbit.qbitSerialModule
import qbit.test.model.testsSerialModule

class SerializationMappingTest : MappingTest(KSFactorization(qbitSerialModule + testsSerialModule)::ksDestruct)