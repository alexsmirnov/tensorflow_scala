/* Copyright 2017, Emmanouil Antonios Platanios. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.platanios.tensorflow.api.ops.training.optimizers

import org.platanios.tensorflow.api.ops.{Basic, Math, Op, Output, OutputIndexedSlices}
import org.platanios.tensorflow.api.ops.variables.Variable

/** Optimizer that implements the gradient descent algorithm.
  *
  * @author Emmanouil Antonios Platanios
  */
case class GradientDescent(
    learningRate: Double, useLocking: Boolean = false, name: String = "GradientDescentOptimizer") extends Optimizer {
  private[this] var learningRateTensor: Output = _

  private[this] def getLearningRate(variable: Variable): Output = {
    if (learningRateTensor == null)
      throw new IllegalStateException("Method 'prepare' has not been called on this optimizer.")
    Math.cast(learningRateTensor, variable.dataType)
  }

  override def prepare(): Unit = {
    learningRateTensor = Basic.constant(learningRate, name = "LearningRate")
  }

  override def applyDense(gradient: Output, variable: Variable): Op = {
    GradientDescent.resourceApplyDense(variable, getLearningRate(variable), gradient, useLocking)
  }

  override def applySparse(gradient: OutputIndexedSlices, variable: Variable): Op = {
    variable.assignScatterSub(gradient.indices, -gradient.values * getLearningRate(variable)).op
  }

  override def applySparseDuplicateIndices(gradient: OutputIndexedSlices, variable: Variable): Op = {
    applySparse(gradient, variable)
  }
}

object GradientDescent {
  /** Creates an op that updates the value of `variable` by subtracting `stepSize * gradient` from it.
    *
    * @param  variable   Variable whose value to update.
    * @param  stepSize   Step size to use for the gradient descent update.
    * @param  gradient   Gradient to apply.
    * @param  useLocking If `true`, the subtraction will be protected by a lock. Otherwise, the behavior is undefined,
    *                    but may exhibit less contention.
    * @param  name       Name for the created op.
    * @return Created op.
    */
  private[GradientDescent] def resourceApplyDense(
      variable: Variable, stepSize: Output, gradient: Output, useLocking: Boolean = false,
      name: String = "ResourceApplyGradientDescent"): Op = {
    Op.Builder(opType = "ResourceApplyGradientDescent", name = name)
        .addInput(variable.handle)
        .addInput(stepSize)
        .addInput(gradient)
        .setAttribute("use_locking", useLocking)
        .build()
  }
}
