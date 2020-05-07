package qbit.typing

import kotlinx.serialization.modules.plus
import qbit.factoring.serializatoin.KSFactorizer
import qbit.qbitSerialModule
import qbit.test.model.testsSerialModule

class SerializationMappingTest : MappingTest(
    KSFactorizer(
        qbitSerialModule + testsSerialModule
    )::factor)