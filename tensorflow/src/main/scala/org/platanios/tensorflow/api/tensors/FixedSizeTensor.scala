package org.platanios.tensorflow.api.tensors

import java.nio.ByteBuffer

import org.platanios.tensorflow.api._
import org.platanios.tensorflow.api.Exception.InvalidDataTypeException

/**
  * @author Emmanouil Antonios Platanios
  */
class FixedSizeTensor private[tensors] (
    override val dataType: FixedSizeDataType, override val shape: Shape, override val buffer: ByteBuffer,
    override val order: Order = DEFAULT_TENSOR_MEMORY_STRUCTURE_ORDER)
    extends Tensor {
  // TODO: Remove these from the plain tensor --- use them only for slices.
  // TODO: What about unknown rank?
  private[api] val underlyingTensorDimensions: Array[Int] = shape.asArray
  val beginOffsets: Array[Int] = Array.fill(shape.rank)(0)
  val endOffsets  : Array[Int] = shape.asArray
  val strides     : Array[Int] = Array.fill(shape.rank)(1)

  override private[api] def flattenedIndex(indices: Array[Int]): Int = {
    order.index(underlyingTensorDimensions, beginOffsets, strides, indices)
  }

  override private[api] def flattenedIndexIterator: Iterator[Int] = {
    order.indexIterator(underlyingTensorDimensions, beginOffsets, endOffsets, strides)
  }

  override private[api] def setElementAtFlattenedIndex[T](
      index: Int, value: T)(implicit evidence: SupportedType[T]): this.type = {
    dataType.putElementInBuffer(buffer, index * dataType.byteSize, dataType.cast(value))
    this
  }

  override private[api] def getElementAtFlattenedIndex(index: Int): dataType.ScalaType = {
    dataType.getElementFromBuffer(buffer, index * dataType.byteSize)
  }

  override def fill[T](value: T)(implicit evidence: SupportedType[T]): this.type = {
    val castedValue = dataType.cast(value)
    for (index <- flattenedIndexIterator)
      dataType.putElementInBuffer(buffer = buffer, index = index, element = castedValue)
    this
  }

  // TODO: Return Tensor objects for contiguous slices.
  override def slice(indexers: Indexer*): Tensor = {
    if (shape.rank == 0 && indexers.length == 1
        && indexers.head.isInstanceOf[Index] && indexers.head.asInstanceOf[Index].index == 0)
      this
    else {
      val decoded = Indexer.decode(shape, indexers)
      FixedSizeTensorSlice(this, decoded._1, Shape.fromSeq(decoded._2), decoded._3, decoded._4, decoded._5)
    }
  }
  // TODO: Use this for creating slices: Buffer.slice().position(sliceStart).limit(sliceSize)

  override def asNumeric: NumericTensor = dataType match {
    case d: NumericDataType => new NumericTensor(d, shape, buffer, order)
    case _ => throw InvalidDataTypeException(s"Data type '$dataType' of this tensor is not numeric.")
  }

  override def asRealNumeric: RealNumericTensor = dataType match {
    case d: RealNumericDataType => new RealNumericTensor(d, shape, buffer, order)
    case _ => throw InvalidDataTypeException(s"Data type '$dataType' of this tensor is not real numeric.")
  }
}

final case class FixedSizeTensorSlice(
    tensor: FixedSizeTensor, override val underlyingTensorDimensions: Array[Int], override val shape: Shape,
    override val beginOffsets: Array[Int], override val endOffsets: Array[Int], override val strides: Array[Int])
    extends FixedSizeTensor(tensor.dataType, tensor.shape, tensor.buffer, tensor.order) {
  override def rank: Int = shape.rank
  override def numElements: Int = shape.numElements.get

  //  def apply(indexers: Indexer*): Any = {
  //    if (tensor.dataType.byteSize == -1)
  //      throw new IllegalStateException("Cannot index a tensor whose elements have unknown byte size.")
  //    // TODO: Add checks for whether the indexers provided are within bounds.
  //    val elementIndex = tensor.order.index(tensor.shape, this.indexers, indexers: _*) * tensor.dataType.byteSize
  //    tensor.dataType.getElementFromBuffer(buffer = tensor.buffer, index = elementIndex)
  //  }
}
